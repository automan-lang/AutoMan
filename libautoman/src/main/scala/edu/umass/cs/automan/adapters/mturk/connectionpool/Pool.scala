package edu.umass.cs.automan.adapters.mturk.connectionpool

import java.text.SimpleDateFormat
import java.util.concurrent.PriorityBlockingQueue
import java.util.{Date, UUID}
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.mock.MockRequesterService
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import edu.umass.cs.automan.core.util.{Utilities, Stopwatch}

class Pool(backend: RequesterService, sleep_ms: Int, mock_service: Option[MockRequesterService]) {
  type HITID = String
  type BatchKey = (String,BigDecimal,Int)   // (group_id, cost, timeout); uniquely identifies a batch
  type HITKey = (BatchKey, String)          // (BatchKey, memo_hash); uniquely identifies a HIT

  // work queue
  private val _requests: PriorityBlockingQueue[Message] = new PriorityBlockingQueue[Message]()

  // response data
  private val _responses = scala.collection.mutable.Map[Message, Any]()

  // MTurk-related state
  private var _state = new MTState()

  // worker
  private var _worker_thread: Thread = startWorker()

  def restoreState(state: MTState) : Unit = {
    _state = state
  }

  // API
  def accept(t: Task) : Task = {
    blocking_enqueue[AcceptReq, Task](AcceptReq(t))
  }
  def backend_budget: BigDecimal = {
    blocking_enqueue[BudgetReq, BigDecimal](BudgetReq())
  }
  def cancel(t: Task) : Task = {
    // don't bother to schedule cancellation if the task
    // is not actually running
    if (t.state == SchedulerState.RUNNING) {
      blocking_enqueue[CancelReq, Task](CancelReq(t))
    } else {
      t.copy_as_cancelled()
    }
  }
  def cleanup_qualifications(mtq: MTurkQuestion) : Unit = {
    nonblocking_enqueue[DisposeQualsReq, Unit](DisposeQualsReq(mtq))
  }
  def post(ts: List[Task], exclude_worker_ids: List[String]) : Unit = {
    nonblocking_enqueue[CreateHITReq, Unit](CreateHITReq(ts, exclude_worker_ids))
  }
  def reject(t: Task, correct_answer: String) : Task = {
    blocking_enqueue[RejectReq, Task](RejectReq(t, correct_answer))
  }
  def retrieve(ts: List[Task], current_time: Date) : List[Task] = {
    blocking_enqueue[RetrieveReq, List[Task]](RetrieveReq(ts, current_time))
  }
  def shutdown(): Unit = synchronized {
    nonblocking_enqueue[ShutdownReq, Unit](ShutdownReq())
  }

  // IMPLEMENTATIONS
  private def nonblocking_enqueue[M <: Message, T](req: M) = {
    // put job in queue
    _requests.add(req)
  }
  private def blocking_enqueue[M <: Message, T](req: M) : T = {
    // wait for response
    // while loop is because the JVM is
    // permitted to send spurious wakeups
    while(synchronized { !_responses.contains(req) }) {
      // Note that the purpose of this second lock
      // is to provide blocking semantics
      req.synchronized {
        // enqueue inside sync so that we don't miss notify
        nonblocking_enqueue(req)
        req.wait() // release lock and block until notify is sent
      }
    }

    // return output
    synchronized {
      val ret = _responses(req)
      _responses.remove(req)
      ret.asInstanceOf[T]
    }
  }
  private def startWorker() : Thread = {
    val t = initWorkerThread()
    t.start()
    t
  }
  private def initWorkerThread(): Thread = {
    DebugLog("No worker thread; starting one up.", LogLevel.INFO, LogType.ADAPTER, null)
    val t = new Thread(new Runnable() {
      override def run() {
        while (true) {

          val time = Stopwatch {
            val work_item = _requests.take()

            work_item match {
              case req: ShutdownReq => {
                DebugLog("Connection pool shutdown requested.", LogLevel.INFO, LogType.ADAPTER, null)
                return
              }
              case req: AcceptReq => do_sync_action(req, () => scheduled_accept(req.t))
              case req: BudgetReq => do_sync_action(req, () => scheduled_get_budget())
              case req: CancelReq => do_sync_action(req, () => scheduled_cancel(req.t))
              case req: DisposeQualsReq => do_sync_action(req, () => scheduled_cleanup_qualifications(req.q))
              case req: CreateHITReq => do_sync_action(req, () => scheduled_post(req.ts, req.exclude_worker_ids))
              case req: RejectReq => do_sync_action(req, () => scheduled_reject(req.t, req.correct_answer))
              case req: RetrieveReq => do_sync_action(req, () => scheduled_retrieve(req.ts, req.current_time))
            }
          }

          // rate-limit
          val duration = Math.max(sleep_ms - time.duration_ms, 0)
          if (duration > 0) {
            DebugLog("MTurk connection pool sleeping for " + duration.toString + " milliseconds.", LogLevel.INFO, LogType.ADAPTER, null)
            Thread.sleep(duration)
          } else {
            DebugLog("MTurk connection pool thread yield.", LogLevel.INFO, LogType.ADAPTER, null)
            Thread.`yield`()
          }
        } // exit loop

      }
    })
    t.setName("MTurk Connection Pool Thread")
    t
  }
  private def do_sync_action[T](message: Message, action: () => T) : Unit = {
    message.synchronized {
      // do request
      val response = action()
      // store response
      synchronized {
        _responses += (message -> response)
      }
      // send end-wait notification
      message.notifyAll()
    }
  }
  private def scheduled_accept(t: Task) : Task = {
    DebugLog(
      String.format("Accepting task for question_id = %s",
      t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    _state.getAssignmentOption(t) match {
      case Some(assignment) =>
        backend.approveAssignment(assignment.getAssignmentId, "Thanks!")
        t.copy_as_accepted()
      case None =>
        throw new Exception("Cannot accept non-existent assignment.")
    }
  }
  private def scheduled_cancel(t: Task) : Task = {
    DebugLog(String.format("Cancelling task for question_id = %s",
      t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    val hit_id = _state.getHITID(t)
    val hit_state = _state.getHITState(hit_id)

    // only cancel HIT if it is not already cancelled
    if (!hit_state.isCancelled) {
      backend.forceExpireHIT(hit_state.HITId)
      _state = _state.updateHITStates(hit_id, hit_state.cancel())
    }

    t.copy_as_cancelled()
  }

  /**
   * Create a new HITType on MTurk, with a disqualification if applicable.
   * @param question An AutoMan Question[_]
   * @param batch_key Batch parameters
   */
  private def mturk_registerHITType(question: Question, batch_key: BatchKey) : Unit = {
    val (group_id, cost, worker_timeout) = batch_key

    // get current batch number
    val batch_no = _state.getBatchNo(group_id)

    // create disqualification for batch
    val disqualification = mturk_createQualification(question.asInstanceOf[MTurkQuestion], question.text, question.id, batch_no)

    // whenever we create a new group, we need to add the disqualification to the HITType
    // EXCEPT if it's the very first time the group is posted
    // AND we weren't specifically asked to blacklist any workers
    val quals = if (question.blacklisted_workers.size > 0 || batch_no != 1) {
      disqualification :: question.asInstanceOf[MTurkQuestion].qualifications
    } else {
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
    _state = _state.updateDisqualifications(disqualification.getQualificationTypeId, hittype.id)

    // update hittype map
    _state = _state.updateHITTypes(batch_key, hittype)
  }

  private def mturk_createHIT(ts: List[Task], batch_key: BatchKey, question: Question) : HITState = {
    // get hit_type for batch
    val hit_type = get_or_create_hittype(batch_key, question)

    val hit = backend.createHIT(
      hit_type.id,                                                  // hitTypeId
      null,                                                         // title; defined by HITType
      null,                                                         // description
      null,                                                         // keywords; defined by HITType
      question.asInstanceOf[MTurkQuestion].toXML(randomize = true).toString(),  // question
      null,                                                         // reward; defined by HITType
      null,                                                         // assignmentDurationInSeconds; defined by HITType
      null,                                                         // autoApprovalDelayInSeconds; defined by HITType
      ts.head.timeout_in_s.toLong,                                  // lifetimeInSeconds
      ts.size,                                                      // maxAssignments
      question.id.toString,                                         // requesterAnnotation
      Array[QualificationRequirement](),                            // qualificationRequirements; defined by HITType
      Array[String]())                                              // responseGroup
    // we immediately query the backend for the HIT's complete details
    // because the HIT structure returned by createHIT has a number
    // of uninitialized fields; return new HITState
    val hs = HITState(backend.getHIT(hit.getHITId), ts, hit_type)

    // calculate new HIT key
    val hit_key = (batch_key, question.memo_hash)

    // update HIT key -> HIT ID map
    _state = _state.updateHITIDs(hit_key, hs.HITId)

    // update HIT ID -> HITState map
    _state = _state.updateHITStates(hs.HITId, hs)

    hs
  }

  private def mturk_extendHIT(ts: List[Task], timeout_in_s: Int, hitstate: HITState) : Unit = {
    backend.extendHIT(hitstate.HITId, ts.size, timeout_in_s.toLong)
    // we immediately query the backend for the HIT's complete details
    // to update our cached data

    // update HITState and return
    val hs = hitstate.addNewTasks(backend.getHIT(hitstate.HITId), ts)

    // update hit states with new object
    _state = _state.updateHITStates(hs.HITId, hs)
  }

  /**
   * Returns true if this group_id has never been associated with
   * any work on MTurk.
   * @param group_id The group_id.
   * @return True if the group_id has already scheduled tasks on MTurk.
   */
  private def first_run(group_id: String) : Boolean = {
    _state.isFirstRun(group_id)
  }

  /**
   * Checks that a HITType already exists for the task group;
   * if it does, it returns the associated HITType object,
   * otherwise it creates a HITType on MTurk.
   * @param batch_key A GroupKey tuple that uniquely identifies a batch round.
   * @param question An AutoMan question.
   * @return A HITType
   */
  private def get_or_create_hittype(batch_key: BatchKey, question: Question) : HITType = {
    // when these properties change from what we've seen before
    // (including the possibility that we've never seen any of these
    // tasks before) we need to create a new HITType;
    // Note that simply adding blacklisted/excluded workers to an existing group
    // is not sufficient to trigger the creation of a new HITType, nor do we want
    // it to, because MTurk's extendHIT is sufficient to prevent re-participation
    // for a given HIT.
    val (group_id, _, _) = batch_key

    val firstrun = first_run(group_id)

    if (!_state.hit_types.contains(batch_key)) {
      // update batch counter
      _state = _state.initOrUpdateBatchNo(group_id)

      // request new HITTypeId from MTurk
      mturk_registerHITType(question, batch_key)
    }
    _state.hit_types(batch_key)
  }

  /**
   * This call marshals data to MTurk, updating local state
   * where necessary.
   * @param ts  A List of Tasks to post.
   * @param exclude_worker_ids  A list of worker_ids to exclude (via disqualifications)
   */
  private def scheduled_post(ts: List[Task], exclude_worker_ids: List[String]) : Unit = {
    // One consequence of dealing with groups of tasks is that
    // they may each be associated with a different question; although
    // automan never calls post with heterogeneous set of tasks, we
    // have to allow for the possibility that it does.
    ts.groupBy(_.question).foreach { case (q, qts) =>
      // Our questions are *always* MTurkQuestions
      val mtq = q.asInstanceOf[MTurkQuestion]

      // also, we need to ensure that all the tasks have the same properties
      qts.groupBy{ t => (t.cost,t.worker_timeout)}.foreach { case ((cost,worker_timeout), tz) =>
        // The batch is uniquely determined by group_id, cost, and worker_timeout
        val batch_key: BatchKey = (mtq.group_id, cost, worker_timeout)

        // A HIT is uniquely determined by question_id, cost, and worker_timeout
        val hit_key: HITKey = (batch_key, q.memo_hash)

        // have we already posted a HIT for these tasks?
        if (_state.hit_ids.contains(hit_key)) {
          // if so, get HITState and extend it
          mturk_extendHIT(tz, tz.head.timeout_in_s, _state.getHITState(hit_key))
        } else {
          // if not, post a new HIT on MTurk
          mturk_createHIT(tz, batch_key, q)
        }
      }
    }
  }

  private def scheduled_reject(t: Task, rejection_response: String) : Task = {
    DebugLog(String.format("Rejecting task for question_id = %s",
      t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    _state.getAssignmentOption(t) match {
      case Some(assignment) =>
        backend.rejectAssignment(assignment.getAssignmentId, rejection_response)
        t.copy_as_rejected()
      case None =>
        throw new Exception("Cannot accept non-existent assignment.")
    }
  }

  private def scheduled_retrieve(ts: List[Task], current_time: Date): List[Task] = {
    // 1. eagerly get all HIT assignments
    // 2. pair HIT Assignments with tasks
    // 3. update tasks with answers
    ts.groupBy(Key.BatchKey).map { case (batch_key, bts) =>
      // get HITType for BatchKey
//      val hittype = _state.getHITType(batch_key)

      val hittype = try {
        _state.getHITType(batch_key)
      } catch {
        case t:Throwable =>
          println("Hi.")
          throw t
      }

      // iterate through all HITs for this HITType
      // pair all assignments with tasks, yielding a new collection of HITStates
      val updated_hss = _state.getHITIDsForBatch(batch_key).map { hit_id =>
        val hit_state = _state.getHITState(hit_id)

        // get all of the assignments for this HIT
        val assns = backend.getAllAssignmentsForHIT(hit_state.HITId)

        // pair with the HIT's tasks and return new HITState
        hit_state.HITId -> hit_state.matchAssignments(assns, mock_service)
      }

      // update HITState map all at once
      _state = _state.updateHITStates(updated_hss)

      // return answered tasks, updating tasks only
      // with those events that do not happen in the future
      answer_tasks(bts, batch_key, current_time)
    }.flatten.toList
  }

  private def answer_tasks(ts: List[Task], batch_key: BatchKey, current_time: Date) : List[Task] = {
    val group_id = batch_key._1
    val ct = Utilities.dateToCalendar(current_time)

    // group by HIT
    ts.groupBy(Key.HITKeyForBatch(batch_key,_)).map { case (hit_key, hts) =>
      // get HITState for this set of tasks
      val hs = _state.getHITState(hit_key)

      // start by granting Qualifications, where appropriate
      mturk_grantQualifications(hs)

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
              if (!_state.worker_whitelist.contains(worker_id, group_id)) {
                _state = _state.updateWorkerWhitelist(worker_id, group_id, hs.hittype.id)
                val disqualification_id = hs.hittype.disqualification.getQualificationTypeId
                backend.assignQualification(disqualification_id, worker_id, _state.getBatchNo(hs.hittype.group_id), false)
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
              val whitelisted_ht_id = _state.getWhitelistedHITType(worker_id, group_id)
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
    }.flatten.toList
  }

  private def scheduled_get_budget(): BigDecimal = {
    DebugLog("Getting budget.", LogLevel.INFO, LogType.ADAPTER, null)
    backend.getAccountBalance
  }

  private def scheduled_cleanup_qualifications(q: MTurkQuestion) : Unit = {
    q.qualifications.foreach { qual =>
      backend.disposeQualificationType(qual.getQualificationTypeId)
    }
  }

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

  private def mturk_grantQualifications(hitstate: HITState) : Unit = {
    // get all requests for this HIT's group qualification
    val requests = hitstate.hittype.quals.map { qual =>
      backend.getAllQualificationRequests(qual.getQualificationTypeId)
    }.flatten

    requests.foreach { request =>
      // "SubjectId" === "WorkerId"
      val worker_id = request.getSubjectId

      // the HITType being requested
      val hit_type_id = if(_state.disqualifications.contains(request.getQualificationTypeId)) {
        _state.getHITTypeIDforQualificationTypeID(request.getQualificationTypeId)
      } else {
        throw new Exception("User-defined qualifications not yet supported.")
      }

      // the group_id for this HITType
      val group_id = hitstate.hittype.group_id

      if (_state.worker_whitelist.contains(worker_id, group_id)) {
        if (_state.getWhitelistedHITType(worker_id, group_id) != hit_type_id) {
          backend.rejectQualificationRequest(request.getQualificationRequestId,
            "You have already requested a qualification or submitted work for an associated HITType " +
              "that disqualifies you from participating in this HITType."
          )
        } else {
          backend.rejectQualificationRequest(request.getQualificationRequestId,
            "You cannot request this qualification more than once."
          )
        }
      } else {
        _state = _state.updateWorkerWhitelist(worker_id, group_id, hit_type_id)
        backend.grantQualification(request.getQualificationRequestId, _state.getBatchNo(hitstate.hittype.group_id))
      }
    }
  }

  private def mturk_createQualification(q: MTurkQuestion, title: String, question_id: UUID, batch_no: Int) : QualificationRequirement = {
    // get a simply-formatted date
    val sdf = new SimpleDateFormat("yyyy-MM-dd:z")
    val datestr = sdf.format(new Date())

    DebugLog("Creating disqualification.",LogLevel.INFO,LogType.ADAPTER,question_id)
    val qualtxt = String.format("AutoMan automatically generated Disqualification (title: %s, date: %s)", title, datestr)
    val qual : QualificationType = backend.createQualificationType("AutoMan " + UUID.randomUUID(), "automan", qualtxt)
    new QualificationRequirement(qual.getQualificationTypeId, Comparator.EqualTo, batch_no, null, false)
  }
}
