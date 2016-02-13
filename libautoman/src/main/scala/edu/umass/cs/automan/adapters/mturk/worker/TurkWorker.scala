package edu.umass.cs.automan.adapters.mturk.worker

import java.text.SimpleDateFormat
import java.util.concurrent.PriorityBlockingQueue
import java.util.{Date, UUID}
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.logging.MTMemo
import edu.umass.cs.automan.adapters.mturk.mock.MockRequesterService
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key.{HITKey, BatchKey}
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import edu.umass.cs.automan.core.util.{Utilities, Stopwatch}

class TurkWorker(backend: RequesterService, sleep_ms: Int, mock_service: Option[MockRequesterService], memo_handle: MTMemo) {
  type HITID = String
  type BatchKey = (String,BigDecimal,Int)   // (group_id, cost, timeout); uniquely identifies a batch
  type HITKey = (BatchKey, String)          // (BatchKey, memo_hash); uniquely identifies a HIT

  // work queue
  private val _requests: PriorityBlockingQueue[Message] = new PriorityBlockingQueue[Message]()

  // response data
  private val _responses = scala.collection.mutable.Map[Message, Any]()

  // MTurk-related state
  private var _state = memo_handle.restore_mt_state(backend) match {
    case Some(state) => state
    case None => new MTState()
  }

  // worker exit condition
  private var _workerExitState: Option[Throwable] = None

  // worker
  startWorker()

  // API
  def accept(ts: List[Task]) : Option[List[Task]] = {
    blocking_enqueue[AcceptReq, List[Task]](AcceptReq(ts))
  }
  def backend_budget: Option[BigDecimal] = {
    blocking_enqueue[BudgetReq, BigDecimal](BudgetReq())
  }
  def cancel(ts: List[Task]) : Option[List[Task]] = {
    // don't bother to schedule cancellation if the task
    // is not actually running
    val (ts_cancels,ts_noncancels) = ts.partition(_.state == SchedulerState.RUNNING)
    val ts_cancelled_opt = blocking_enqueue[CancelReq, List[Task]](CancelReq(ts))
    ts_cancelled_opt match {
      case Some(ts_cancelled) => Some(ts_cancelled ::: ts_noncancels.map(_.copy_as_cancelled()))
      case None => None
    }
  }
  def cleanup_qualifications(mtq: MTurkQuestion) : Unit = {
    nonblocking_enqueue[DisposeQualsReq, Unit](DisposeQualsReq(mtq))
  }
  def post(ts: List[Task], exclude_worker_ids: List[String]) : Option[List[Task]] = {
    blocking_enqueue[CreateHITReq, List[Task]](CreateHITReq(ts, exclude_worker_ids))
  }
  def reject(ts_reasons: List[(Task, String)]) : Option[List[Task]] = {
    blocking_enqueue[RejectReq, List[Task]](RejectReq(ts_reasons))
  }
  def retrieve(ts: List[Task], current_time: Date) : Option[List[Task]] = {
    blocking_enqueue[RetrieveReq, List[Task]](RetrieveReq(ts, current_time))
  }
  def shutdown(): Unit = {
    nonblocking_enqueue[ShutdownReq, Unit](ShutdownReq())
  }

  // IMPLEMENTATIONS
  private def nonblocking_enqueue[M <: Message, T](req: M) = synchronized {
    // put job in queue
    _requests.add(req)
  }
  private def blocking_enqueue[M <: Message, T](req: M) : Option[T] = {
    // wait for response
    // while loop is because the JVM is
    // permitted to send spurious wakeups;
    // also enture that backend is still running
    while(synchronized { !_responses.contains(req) }
          && _workerExitState.isEmpty) {
      var enqueued = false
      // Note that the purpose of this second lock
      // is to provide blocking semantics
      req.synchronized {
        // enqueue inside sync so that we don't miss notify
        if (!enqueued) {
          nonblocking_enqueue(req)
          enqueued = true
        }
        req.wait() // release lock and block until notify is sent
      }
    }

    // return output
    synchronized {
      // check that loop did not end due to fatal error
      _workerExitState match {
        case None => {
          val ret = _responses(req).asInstanceOf[T]
          _responses.remove(req)
          Some(ret)
        }
        case Some(throwable) => None
      }

    }
  }
  private def startWorker() : Thread = {
    val t = initWorkerThread()
    t.start()
    t
  }
  private def initWorkerThread(): Thread = {
    DebugLog("No worker thread; starting one up.", LogLevelInfo(), LogType.ADAPTER, null)
    val t = new Thread(new Runnable() {
      override def run() {
        while (true) {

          val time = Stopwatch {
            val work_item = _requests.take()

            try {
              work_item match {
                case req: ShutdownReq => {
                  DebugLog("Connection pool shutdown requested.", LogLevelInfo(), LogType.ADAPTER, null)
                  return
                }
                case req: AcceptReq => do_sync_action(req, () => scheduled_accept(req.ts))
                case req: BudgetReq => do_sync_action(req, () => scheduled_get_budget())
                case req: CancelReq => do_sync_action(req, () => scheduled_cancel(req.ts))
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
          val duration = Math.max(sleep_ms - time.duration_ms, 0)
          if (duration > 0) {
            DebugLog("MTurk connection pool sleeping for " + duration.toString + " milliseconds.", LogLevelInfo(), LogType.ADAPTER, null)
            Thread.sleep(duration)
          } else {
            DebugLog("MTurk connection pool thread yield.", LogLevelInfo(), LogType.ADAPTER, null)
            Thread.`yield`()
          }
        } // exit loop
      }
    })
    t.setName("MTurk Worker Thread")
    t
  }

  private def failureCleanup(failed_request: Message, throwable: Throwable): Unit = {
    // cleanup
    synchronized {
      // set exit state
      _workerExitState = Some(throwable)

      // unblock owner of failed request
      failed_request.synchronized {
        failed_request.notifyAll()
      }

      // unblock remaining threads
      while (!_requests.isEmpty) {
        val req = _requests.take()
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
      _responses.put(message, response)
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
            backend.approveAssignment(assignment.getAssignmentId, "Thanks!")
            t.copy_as_accepted()
          case None =>
            throw new Exception("Cannot accept non-existent assignment.")
        }
      }

      // save point
      memo_handle.save(question, List.empty, accepts)

      accepts
    }.toList
    // no mt state to update here
  }
  private def scheduled_cancel(ts: List[Task]) : List[Task] = {
    var internal_state = _state

    ts.groupBy(_.question).flatMap { case (question, tasks) =>
      DebugLog(s"Canceling ${tasks.size} tasks.", LogLevelInfo(), LogType.ADAPTER, question.id)

      val cancels = tasks.map { t =>
        val hit_id = internal_state.getHITID(t)
        val hit_state = internal_state.getHITState(hit_id)

        // only cancel HIT if it is not already cancelled
        if (!hit_state.isCancelled) {
          backend.forceExpireHIT(hit_state.HITId)
          internal_state = internal_state.updateHITStates(hit_id, hit_state.cancel())
        }

        t.copy_as_cancelled()
      }

      // save point
      memo_handle.save(question, List.empty, cancels)
      memo_handle.save_mt_state(internal_state)

      // update adapter state
      _state = internal_state

      cancels
    }.toList
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
          internal_state = TurkWorker.mturk_extendHIT(tz, tz.head.timeout_in_s, hit_key, internal_state, backend)
        } else {
          // if not, post a new HIT on MTurk
          internal_state = TurkWorker.mturk_createHIT(tz, group_key, q, internal_state, backend)
        }

        // mark as running
        tz.map(_.copy_as_running())
      }.toList

      // save point
      memo_handle.save(q, running, List.empty)

      running
    }.toList

    // mt save point
    memo_handle.save_mt_state(internal_state)

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
            backend.rejectAssignment(assignment.getAssignmentId, reason)
            t.copy_as_rejected()
          case None =>
            throw new Exception("Cannot accept non-existent assignment.")
        }
      }

      // save point
      memo_handle.save(question, List.empty, rejects)

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
        val assns = backend.getAllAssignmentsForHIT(hit_state.HITId)

        // pair with the HIT's tasks and return new HITState
        hit_state.HITId -> hit_state.matchAssignments(assns, mock_service)
      }

      // update HITState map all at once
      internal_state = internal_state.updateHITStates(updated_hss)

      // return answered tasks, updating tasks only
      // with those events that do not happen in the future
      val (answered, state2) =
        TurkWorker.answer_tasks(
          bts,
          batch_key,
          current_time,
          internal_state,
          backend,
          mock_service
        )
      internal_state = state2

      answered
    }.toList

    // save point
    ts2.groupBy(_.question).foreach { case (question, tasks) =>
      memo_handle.save(question, List.empty, tasks)
    }
    memo_handle.save_mt_state(internal_state)

    // update state
    _state = internal_state

    ts2
  }

  private def scheduled_get_budget(): BigDecimal = {
    DebugLog("Getting budget.", LogLevelInfo(), LogType.ADAPTER, null)
    backend.getAccountBalance
  }

  private def scheduled_cleanup_qualifications(q: MTurkQuestion) : Unit = {
    q.qualifications.foreach { qual =>
      backend.disposeQualificationType(qual.getQualificationTypeId)
    }
  }

}

object TurkWorker {

  private def mtquestion_for_tasks(ts: List[Task]) : MTurkQuestion = {
    // determine which MT question we've been asked about
    question_for_tasks(ts) match {
      case mtq: MTurkQuestion => mtq
      case _ => throw new Exception("MTurkAdapter can only operate on Tasks for MTurkQuestions.")
    }
  }

  private def question_for_tasks(ts: List[Task]) : Question = {
    // determine which question we've been asked about
    val tg = ts.groupBy(_.question)
    if(tg.size != 1) {
      throw new Exception("MTurkAdapter can only process groups of Tasks for the same Question.")
    }
    tg.head._1
  }

  private def mturk_grantQualifications(hitstate: HITState, state: MTState, backend: RequesterService) : MTState = {
    var internal_state = state

    // get all requests for this HIT's group qualification
    val requests = hitstate.hittype.quals.flatMap { qual =>
      backend.getAllQualificationRequests(qual.getQualificationTypeId)
    }

    requests.foreach { request =>
      // "SubjectId" === "WorkerId"
      val worker_id = request.getSubjectId

      // the HITType being requested
      val hit_type_id = if(internal_state.disqualifications.contains(request.getQualificationTypeId)) {
        internal_state.getHITTypeIDforQualificationTypeID(request.getQualificationTypeId)
      } else {
        throw new Exception("User-defined qualifications not yet supported.")
      }

      // the group_id for this HITType
      val group_id = hitstate.hittype.group_id

      // if the worker is known to us, then we've already granted them a disqualification
      if (internal_state.worker_whitelist.contains(worker_id, group_id)) {
        // if that disqualification is not the same as the one they're asking for, sorry, reject;
        // granting this would violate i.i.d. guarantee
        if (internal_state.getWhitelistedHITType(worker_id, group_id) != hit_type_id) {
          backend.rejectQualificationRequest(request.getQualificationRequestId,
            "You have already requested a qualification or submitted work for an associated HITType " +
              "that disqualifies you from participating in this HITType."
          )
        // otherwise, they're requesting something we've already granted; reject
        } else {
          backend.rejectQualificationRequest(request.getQualificationRequestId,
            "You cannot request this qualification more than once."
          )
        }
      } else {
        // if we don't know them, record the user and grant their disqualification request
        internal_state = internal_state.updateWorkerWhitelist(worker_id, group_id, hit_type_id)
        // get the BatchKey associated with the HITType; guaranteed to exist
        val batchKey = internal_state.getBatchKeyByHITTypeId(hit_type_id).get
        // get the batch_no associated with the BatchKey; guaranteed to exist
        backend.grantQualification(request.getQualificationRequestId, internal_state.getBatchNo(batchKey).get)
      }
    }

    internal_state
  }

  private def mturk_createQualification(q: MTurkQuestion, title: String, question_id: UUID, batch_no: Int, backend: RequesterService) : QualificationRequirement = {
    // get a simply-formatted date
    val sdf = new SimpleDateFormat("yyyy-MM-dd:z")
    val datestr = sdf.format(new Date())

    DebugLog("Creating disqualification.",LogLevelInfo(),LogType.ADAPTER,question_id)
    val qualtxt = String.format("AutoMan automatically generated Disqualification (title: %s, date: %s, question_id: %s)", title, datestr, question_id)
    val qual = backend.createQualificationType("AutoMan " + UUID.randomUUID(), "automan", qualtxt)
    new QualificationRequirement(qual.getQualificationTypeId, Comparator.EqualTo, batch_no, null, false)
  }

  private def answer_tasks(ts: List[Task], batch_key: BatchKey, current_time: Date, state: MTState, backend: RequesterService, mock_service: Option[MockRequesterService]) : (List[Task],MTState) = {
    val ct = Utilities.dateToCalendar(current_time)
    var internal_state = state
    val group_id = batch_key._1

    // group by HIT
    val answered = ts.groupBy(Key.HITKeyForBatch(batch_key,_)).flatMap { case (hit_key, hts) =>
      // get HITState for this set of tasks
      val hs = internal_state.getHITState(hit_key)

      // start by granting Qualifications, where appropriate
      internal_state = mturk_grantQualifications(hs, internal_state, backend)

      hts.map { t =>
        hs.getAssignmentOption(t) match {
          // when a task is paired with an answer
          case Some(assignment) =>
            // only update task object if the task isn't already answered
            // and if the answer actually happens in the past (ugly hack for mocks)
            if (t.state == SchedulerState.RUNNING && !assignment.getSubmitTime.after(ct)) {
              // get worker_id
              val worker_id = assignment.getWorkerId

              // update the worker whitelist and grant qualification (disqualifiaction)
              // if this is the first time we've ever seen this worker
              if (!internal_state.worker_whitelist.contains(worker_id, group_id)) {
                internal_state = internal_state.updateWorkerWhitelist(worker_id, group_id, hs.hittype.id)
                val disqualification_id = hs.hittype.disqualification.getQualificationTypeId
                backend.assignQualification(disqualification_id, worker_id, internal_state.getBatchNo(Key.BatchKey(t)).get, false)
              }

              // process answer
              val ans = assignment.getAnswer
              val xml = scala.xml.XML.loadString(ans)
              val prelim_answer = t.question.asInstanceOf[MTurkQuestion].fromXML(xml)
              val answer = t.question.before_filter(prelim_answer.asInstanceOf[t.question.A])

              // it is possible, although unlikely, that a worker could submit
              // work twice for the same HIT, if the following scenario occurs:
              // 1. HIT A in HITGroup #1 times-out, causing AutoMan to post HITGroup #2 containing a second round of HIT A
              // 2. Worker w asks for and receives a Qualification for HITGroup #2
              // 3. Worker w submits work to HITGroup #1 for HIT B (not HIT A).
              // 4. HIT B times out, causing AutoMan to post a second round of HIT B to HITGroup #2.
              // 5. Worker w submits work for HITGroup #2.
              // Since this is unlikely, and violates the i.i.d. guarantee that
              // the Scheduler requires, we consider this a duplicate
              val whitelisted_ht_id = internal_state.getWhitelistedHITType(worker_id, group_id)
              if (whitelisted_ht_id != hs.hittype.id) {
                // immediately revoke the qualification in the other group;
                // we'll deal with duplicates later
                backend.revokeQualification(whitelisted_ht_id, worker_id,
                  "For quality control purposes, qualification " + whitelisted_ht_id +
                    " was revoked because you submitted related work for HIT " + hs.HITId +
                    ".  This is for our own bookkeeping purposes and is not a reflection on the quality of your work. " +
                    "We apologize for the inconvenience that this may cause and we encourage you to continue " +
                    "your participation in our HITs."
                )
              }

              // mark assignment as ANSWERED if we're running in mock mode
              mock_service match {
                case Some(ms) => ms.takeAssignment(assignment.getAssignmentId)
                case None => ()
              }

              t.copy_with_answer(answer.asInstanceOf[t.question.A], worker_id)
            } else {
              t
            }
          // when a task is not paired with an answer
          case None => t
        }
      }
    }.toList

    (answered, internal_state)
  }

  /**
   * Create a new HITType on MTurk, with a disqualification if applicable.
   * @param question An AutoMan Question[_]
   * @param batch_key Batch parameters
   */
  private def mturk_registerHITType(question: Question, batch_key: BatchKey, state: MTState, backend: RequesterService) : MTState = {
    var internal_state = state

    DebugLog("Registering new HIT Type for batch key = " + batch_key, LogLevelDebug(), LogType.ADAPTER, question.id)

    val (group_id, cost, worker_timeout) = batch_key

    // update batch counter
    internal_state = internal_state.updateBatchNo(batch_key)

    // get just-created batch number; guaranteed to exist because we just created it
    val batch_no = internal_state.getBatchNo(batch_key).get

    // create disqualification for batch
    val disqualification = mturk_createQualification(question.asInstanceOf[MTurkQuestion], question.text, question.id, batch_no, backend)
    DebugLog(s"Created disqualification ${disqualification.getQualificationTypeId} for batch key = " + batch_key, LogLevelDebug(), LogType.ADAPTER, question.id)

    // whenever we create a new group, we need to add the disqualification to the HITType
    // EXCEPT if it's the very first time the group is posted
    // AND we weren't specifically asked to blacklist any workers
    val quals = if (question.blacklisted_workers.nonEmpty || batch_no != 1) {
      DebugLog(s"Batch #${batch_no} run, not using disqualification ${disqualification.getQualificationTypeId} for batch " + batch_key, LogLevelDebug(), LogType.ADAPTER, question.id)
      disqualification :: question.asInstanceOf[MTurkQuestion].qualifications
    } else {
      DebugLog(s"Batch #${batch_no} run, using all ${question.asInstanceOf[MTurkQuestion].qualifications.size} qualifications for batch " + batch_key, LogLevelDebug(), LogType.ADAPTER, question.id)
      question.asInstanceOf[MTurkQuestion].qualifications
    }

    val hit_type_id = backend.registerHITType(
      (30 * 24 * 60 * 60).toLong,                                   // 30 days
      worker_timeout.toLong,                                        // amount of time the worker has to complete the task
      cost.toDouble,                                                // cost in USD
      question.title,                                               // title
      question.asInstanceOf[MTurkQuestion].keywords.mkString(","),  // keywords
      question.asInstanceOf[MTurkQuestion].description,             // description
      quals.toArray                                                 // no quals initially
    )
    val hittype = HITType(hit_type_id, quals, disqualification, group_id)

    // update disqualification map
    internal_state = internal_state.updateDisqualifications(disqualification.getQualificationTypeId, hittype.id)

    // update hittype map
    internal_state = internal_state.updateHITTypes(batch_key, hittype)

    internal_state
  }

  private def mturk_createHIT(ts: List[Task], batch_key: BatchKey, question: Question, state: MTState, backend: RequesterService) : MTState = {
    var internal_state = state

    // get hit_type for batch
    val (hit_type,state2) = get_or_create_hittype(batch_key, question, internal_state, backend)
    internal_state = state2

    // render XML
    val xml = question.asInstanceOf[MTurkQuestion].toXML(randomize = true).toString()
    DebugLog("Posting task XML:\n" + xml.toString, LogLevelDebug(), LogType.ADAPTER, question.id)

    val hit = backend.createHIT(
      hit_type.id,                        // hitTypeId
      null,                               // title; defined by HITType
      null,                               // description
      null,                               // keywords; defined by HITType
      xml,                                // question xml
      null,                               // reward; defined by HITType
      null,                               // assignmentDurationInSeconds; defined by HITType
      null,                               // autoApprovalDelayInSeconds; defined by HITType
      ts.head.timeout_in_s.toLong,        // lifetimeInSeconds
      ts.size,                            // maxAssignments
      question.id.toString,               // requesterAnnotation
      Array[QualificationRequirement](),  // qualificationRequirements; defined by HITType
      Array[String]())                    // responseGroup
    // we immediately query the backend for the HIT's complete details
    // because the HIT structure returned by createHIT has a number
    // of uninitialized fields; return new HITState
    val hs = HITState(backend.getHIT(hit.getHITId), ts, hit_type)

    // calculate new HIT key
    val hit_key = (batch_key, question.memo_hash)

    DebugLog(s"Creating new HIT with ID ${hs.HITId} for batch key ${batch_key} and ${ts.size} assignments.", LogLevelDebug(), LogType.ADAPTER, question.id)

    // we update the state like this so that inconsistent state snapshots are not possible
    // update HIT key -> HIT ID map
    internal_state = internal_state.updateHITIDs(hit_key, hs.HITId)

    // update HIT ID -> HITState map
    internal_state.updateHITStates(hs.HITId, hs)
  }

  private def mturk_extendHIT(ts: List[Task], timeout_in_s: Int, hit_key: HITKey, state: MTState, backend: RequesterService) : MTState = {
    val internal_state = state

    val hitstate = internal_state.getHITState(hit_key)

    // MTurk does not allow expiration dates sooner than
    // 60 seconds into the future
    val expiry_s = Math.max(60, timeout_in_s).toLong

    DebugLog(s"Extending HIT ID ${hitstate.HITId} with ${ts.size} new assignments and timeout ${expiry_s} sec", LogLevelDebug(), LogType.ADAPTER, ts.head.question.id)

    // Note that extending HITs is only useful when the only
    // parameters that can change are the 1) number of assignments and
    // the 2) expiration date.
    backend.extendHIT(hitstate.HITId, ts.size, expiry_s)
    // we immediately query the backend for the HIT's complete details
    // to update our cached data

    // update HITState and return
    val hs = hitstate.addNewTasks(backend.getHIT(hitstate.HITId), ts)

    // update hit states with new object
    internal_state.updateHITStates(hs.HITId, hs)
  }

  /**
   * Checks that a HITType already exists for the task group;
   * if it does, it returns the associated HITType object,
   * otherwise it creates a HITType on MTurk.
   * @param batch_key A GroupKey tuple that uniquely identifies a batch round.
   * @param question An AutoMan question.
   * @return A HITType
   */
  private def get_or_create_hittype(batch_key: BatchKey, question: Question, state: MTState, backend: RequesterService) : (HITType, MTState) = {
    var internal_state = state

    // when these properties change from what we've seen before
    // (including the possibility that we've never seen any of these
    // tasks before) we need to create a new HITType;
    // Note that simply adding blacklisted/excluded workers to an existing group
    // is not sufficient to trigger the creation of a new HITType, nor do we want
    // it to, because MTurk's extendHIT is sufficient to prevent re-participation
    // for a given HIT.
    val (group_id, _, _) = batch_key

    if (!internal_state.hit_types.contains(batch_key)) {
      // request new HITTypeId from MTurk
      internal_state = mturk_registerHITType(question, batch_key, internal_state, backend)
    } else {
      DebugLog(s"Reusing HITType with ID ${internal_state.hit_types(batch_key).id} for batch key ${batch_key}.", LogLevelInfo(), LogType.ADAPTER, question.id)
    }
    (internal_state.hit_types(batch_key), internal_state)
  }
}
