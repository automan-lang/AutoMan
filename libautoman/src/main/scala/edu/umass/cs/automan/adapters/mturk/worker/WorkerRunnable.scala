package edu.umass.cs.automan.adapters.mturk.worker

import java.util.Date
import java.util.concurrent.PriorityBlockingQueue
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key._
import edu.umass.cs.automan.core.logging.{LogLevelDebug, LogType, LogLevelInfo, DebugLog}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import edu.umass.cs.automan.core.util.Stopwatch

/**
  * This is a separate class to ensure that state changes are always managed
  * by a single thread.
  * @param tw The parent TurkWorker
  * @param requests A request queue.
  * @param responses A response data structure.
  */
class WorkerRunnable(tw: TurkWorker,
                     requests: PriorityBlockingQueue[Message],
                     responses: scala.collection.mutable.Map[Message, Any]) extends Runnable {
  // MTurk-related state
  private var _state = tw.memo_handle.restore_mt_state(tw.backend) match {
    case Some(state) => state
    case None => new MTState()
  }

  override def run() {
    while (true) {

      val time = Stopwatch {
        val work_item = requests.take()

        try {
          work_item match {
            case req: ShutdownReq => {
              DebugLog("Connection pool shutdown requested.", LogLevelInfo(), LogType.ADAPTER, null)
              return
            }
            case req: AcceptReq => do_sync_action(req, () => scheduled_accept(req.ts))
            case req: BudgetReq => do_sync_action(req, () => scheduled_get_budget())
            case req: CancelReq => do_sync_action(req, () => scheduled_cancel(req.ts, req.toState))
            case req: DisposeQualsReq => do_sync_action(req, () => scheduled_cleanup_qualifications(req.q))
            case req: CreateHITReq => do_sync_action(req, () => scheduled_post(req.ts, req.exclude_worker_ids))
            case req: RejectReq => do_sync_action(req, () => scheduled_reject(req.ts_reasons))
            case req: RetrieveReq => do_sync_action(req, () => scheduled_retrieve(req.ts, req.current_time))
          }
        } catch {
          case t: Throwable => {
            failureCleanup(work_item, t)
            throw t
          }
        }
      }

      // rate-limit
      val duration = Math.max(tw.sleep_ms - time.duration_ms, 0)
      if (duration > 0) {
        DebugLog("MTurk connection pool sleeping for " + duration.toString + " milliseconds.", LogLevelInfo(), LogType.ADAPTER, null)
        Thread.sleep(duration)
      } else {
        DebugLog("MTurk connection pool thread yield.", LogLevelInfo(), LogType.ADAPTER, null)
        Thread.`yield`()
      }
    } // exit loop
  }

  private def failureCleanup(failed_request: Message, throwable: Throwable): Unit = {
    // cleanup
    tw.synchronized {
      // set exit state
      tw._workerExitState = Some(throwable)

      // unblock owner of failed request
      failed_request.synchronized {
        failed_request.notifyAll()
      }

      // unblock remaining threads
      while (!requests.isEmpty) {
        val req = requests.take()
        req.synchronized {
          req.notifyAll()
        }
      }
    }
  }

  private def do_sync_action[T](message: Message, action: () => T) : Unit = {
    // do request
    val response = action()

    message.synchronized {
      // store response
      responses.put(message, response)
      // send end-wait notification
      message.notifyAll()
    }
  }

  private def scheduled_accept(ts: List[Task]) : List[Task] = {
    val internal_state = _state

    ts.groupBy(_.question).flatMap { case (question, tasks) =>
      DebugLog(s"Accepting ${tasks.size} tasks.", LogLevelInfo(), LogType.ADAPTER, question.id)

      val accepts = tasks.map { t =>
        internal_state.getAssignmentOption(t) match {
          case Some(assignment) =>
            DebugLog(
              s"Accepting task ${t.task_id} with assignmentId ${assignment.getAssignmentId} and answer '${t.answer.getOrElse("n/a")}'.",
              LogLevelDebug(),
              LogType.ADAPTER,
              t.question.id)
            tw.backend.approveAssignment(assignment.getAssignmentId, "Thanks!")
            t.copy_as_accepted()
          case None =>
            throw new Exception("Cannot accept non-existent assignment.")
        }
      }

      // save point
      tw.memo_handle.save(question, List.empty, accepts)

      accepts
    }.toList
    // no mt state to update here
  }
  private def scheduled_cancel(ts: List[Task], toState: SchedulerState.Value) : List[Task] = {
    var internal_state = _state

    val stateChanger = toState match {
      case SchedulerState.CANCELLED => (t: Task) => t.copy_as_cancelled()
      case SchedulerState.TIMEOUT => (t: Task) => t.copy_as_timeout()
      case SchedulerState.DUPLICATE => (t: Task) => t.copy_as_duplicate()
      case _ => throw new Exception(s"Invalid target state ${toState} for cancellation request.")
    }

    // don't bother to contact MTurk to cancel tasks that aren't running
    val (to_cancel,dont_bother) = ts.partition(t => t.state == SchedulerState.RUNNING || t.state == SchedulerState.READY)

    val cancelled = to_cancel.groupBy(_.question).flatMap { case (question, tasks) =>
      DebugLog(s"Canceling ${tasks.size} tasks.", LogLevelInfo(), LogType.ADAPTER, question.id)

      val cancels = tasks.map { t =>
        val hit_id = internal_state.getHITID(t)
        val hit_state = internal_state.getHITState(hit_id)

        // only cancel HIT if it is not already cancelled
        if (!hit_state.isCancelled) {
          DebugLog(
            s"Canceling task ${t.task_id}.",
            LogLevelDebug(),
            LogType.ADAPTER,
            t.question.id)
          tw.backend.forceExpireHIT(hit_state.HITId)
          internal_state = internal_state.updateHITStates(hit_id, hit_state.cancel())
        }

        stateChanger(t)
      }

      cancels
    }.toList

    // change state of things we didn't bother to
    // contact MTurk about and concat
    val cancelled_tasks: List[Task] = cancelled ::: dont_bother.map(stateChanger)

    assert(cancelled_tasks.forall(_.state == toState))

    // save point
    cancelled_tasks.groupBy(_.question).foreach { case (q,qts) =>
      tw.memo_handle.save(q, List.empty, qts)
      tw.memo_handle.save_mt_state(internal_state)
    }

    // update adapter state
    _state = internal_state

    cancelled_tasks
  }

  /**
    * This call marshals data to MTurk, updating local state
    * where necessary.
    * @param ts  A List of Tasks to post.
    * @param exclude_worker_ids  A list of worker_ids to exclude (via disqualifications)
    */
  private def scheduled_post(ts: List[Task], exclude_worker_ids: List[String]) : List[Task] = {
    var internal_state = _state

    // One consequence of dealing with groups of tasks is that
    // they may each be associated with a different question; although
    // automan never calls post with heterogeneous set of tasks, we
    // have to allow for the possibility that it does.
    val ts2 = ts.groupBy(_.question).flatMap { case (q, qts) =>
      // Our questions are *always* MTurkQuestions
      val mtq = q.asInstanceOf[MTurkQuestion]

      // also, we need to ensure that all the tasks have the same properties
      val running = qts.groupBy{ t => HITKey(t)}.flatMap { case (hit_key, tz) =>
        val group_key = hit_key._1
        val group_id = group_key._1

        // have we already posted a HIT for these tasks?
        if (internal_state.hit_ids.contains(hit_key)) {
          // if so, get HITState and extend it
          internal_state = MTurkMethods.mturk_extendHIT(tz, tz.head.timeout_in_s, hit_key, internal_state, tw.backend)
        } else {
          // if not, post a new HIT on MTurk
          internal_state = MTurkMethods.mturk_createHIT(tz, group_key, q, internal_state, tw.backend)
        }

        // mark as running
        tz.map(_.copy_as_running())
      }.toList

      // save point
      tw.memo_handle.save(q, running, List.empty)

      running
    }.toList

    // mt save point
    tw.memo_handle.save_mt_state(internal_state)

    // update state
    _state = internal_state

    ts2
  }

  private def scheduled_reject(ts_reasons: List[(Task,String)]) : List[Task] = {
    val internal_state = _state

    ts_reasons.groupBy { case (t,_) => t.question }.flatMap { case (question,tasks_reasons) =>
      DebugLog(s"Rejecting ${tasks_reasons.size} tasks.", LogLevelInfo(), LogType.ADAPTER, question.id)

      val rejects = tasks_reasons.map { case (t,reason) =>
        internal_state.getAssignmentOption(t) match {
          case Some(assignment) =>
            DebugLog(
              s"Rejecting task ${t.task_id} with assignmentId ${assignment.getAssignmentId} and answer '${t.answer.getOrElse("n/a")}'.",
              LogLevelDebug(),
              LogType.ADAPTER,
              t.question.id)
            tw.backend.rejectAssignment(assignment.getAssignmentId, reason)
            t.copy_as_rejected()
          case None =>
            throw new Exception("Cannot accept non-existent assignment.")
        }
      }

      // save point
      tw.memo_handle.save(question, List.empty, rejects)

      rejects
    }.toList
    // no mt state to update here
  }

  private def scheduled_retrieve(ts: List[Task], current_time: Date): List[Task] = {
    var internal_state = _state

    // 1. eagerly get all HIT assignments
    // 2. pair HIT Assignments with tasks
    // 3. update tasks with answers
    val ts2 = ts.groupBy(Key.BatchKey).flatMap { case (batch_key, bts) =>
      // get HITType for BatchKey
      val hittype = internal_state.getHITType(batch_key)

      // iterate through all HITs for this HITType
      // pair all assignments with tasks, yielding a new collection of HITStates
      val updated_hss = internal_state.getHITIDsForBatch(batch_key).map { hit_id =>
        val hit_state = internal_state.getHITState(hit_id)

        // get all of the assignments for this HIT
        val assns = tw.backend.getAllAssignmentsForHIT(hit_state.HITId)

        // pair with the HIT's tasks and return new HITState
        hit_state.HITId -> hit_state.matchAssignments(assns, tw.mock_service)
      }

      // update HITState map all at once
      internal_state = internal_state.updateHITStates(updated_hss)

      // return answered tasks, updating tasks only
      // with those events that do not happen in the future
      val (answered, state2) =
        MTurkMethods.answer_tasks(
          bts,
          batch_key,
          current_time,
          internal_state,
          tw.backend,
          tw.mock_service
        )
      internal_state = state2

      answered
    }.toList

    // save point
    ts2.groupBy(_.question).foreach { case (question, tasks) =>
      tw.memo_handle.save(question, List.empty, tasks)
    }
    tw.memo_handle.save_mt_state(internal_state)

    // update state
    _state = internal_state

    ts2
  }

  private def scheduled_get_budget(): BigDecimal = {
    DebugLog("Getting budget.", LogLevelInfo(), LogType.ADAPTER, null)
    tw.backend.getAccountBalance
  }

  private def scheduled_cleanup_qualifications(q: MTurkQuestion) : Unit = {
    q.qualifications.foreach { qual =>
      tw.backend.disposeQualificationType(qual.getQualificationTypeId)
    }
  }
}
