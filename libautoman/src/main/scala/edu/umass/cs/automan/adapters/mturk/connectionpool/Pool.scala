package edu.umass.cs.automan.adapters.mturk.connectionpool

import java.text.SimpleDateFormat
import java.util.concurrent.PriorityBlockingQueue
import java.util.{Date, UUID}
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.util.Stopwatch

class Pool(backend: RequesterService, sleep_ms: Int) {
  type HITID = String
  type BatchKey = (String,BigDecimal,Int)   // (group_id, cost, timeout); uniquely identifies a batch
  type HITKey = (BatchKey, UUID)            // (BatchKey, question_id); uniquely identifies a HIT

  // worker
  private var _worker_thread: Option[Thread] = None

  // work queue
  private val _work_queue: PriorityBlockingQueue[Message] = new PriorityBlockingQueue[Message]()

  // response data
  private val _responses = scala.collection.mutable.Map[Message, Any]()

  // MTurk-related state

  // key: (group_id,cost,worker_timeout_s)
  // value: a HITTypeId
  private var _hit_types = Map[BatchKey,HITType]()

  // key: HIT ID
  // value: HITState
  private var _hit_states = Map[HITID,HITState]()

  // key: (question_id,cost,worker_timeout_s)
  // value: HIT ID
  private var _hit_ids = Map[HITKey,HITID]()

  // key: worker_id
  // value: HITTypeId
  private var _worker_whitelist = Map[String,String]()

  // key: qualification_id
  // value: HITTypeId
  private var _qualifications = Map[String,String]()

  // API
  def accept[A](t: Thunk[A]) : Thunk[A] = {
    blocking_enqueue(AcceptReq(t)).asInstanceOf[Thunk[A]]
  }
  def backend_budget: BigDecimal = {
    blocking_enqueue(BudgetReq()).asInstanceOf[BigDecimal]
  }
  def cancel[A](t: Thunk[A]) : Thunk[A] = {
    blocking_enqueue(CancelReq(t)).asInstanceOf[Thunk[A]]
  }
  def cleanup_qualifications[A](mtq: MTurkQuestion) : Unit = {
    nonblocking_enqueue(DisposeQualsReq(mtq))
  }
  def post[A](ts: List[Thunk[A]], exclude_worker_ids: List[String]) : Unit = {
    nonblocking_enqueue(CreateHITReq(ts, exclude_worker_ids))
  }
  def reject[A](t: Thunk[A]) : Thunk[A] = {
    blocking_enqueue(RejectReq(t)).asInstanceOf[Thunk[A]]
  }
  def retrieve[A](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    blocking_enqueue(RetrieveReq(ts)).asInstanceOf[List[Thunk[A]]]
  }
  def shutdown(): Unit = synchronized {
    nonblocking_enqueue(ShutdownReq())
  }

  // IMPLEMENTATIONS
  def nonblocking_enqueue[M <: Message, T](req: M) = {
    // put job in queue
    _work_queue.add(req)

    initWorkerIfNeeded()
  }
  def blocking_enqueue[M <: Message, T](req: M) = {
    nonblocking_enqueue(req)

    // wait for response
    // while loop is because the JVM is
    // permitted to send spurious wakeups
    while(synchronized { !_responses.contains(req) }) {
      // Note that the purpose of this second lock
      // is to provide blocking semantics
      req.synchronized {
        req.wait() // block until cancelled thunk is available
      }
    }

    // return output
    synchronized {
      val ret = _responses(req)
      _responses.remove(req)
      ret
    }
  }
  private def initWorkerIfNeeded() : Unit = {
    // if there's no thread already servicing the queue,
    // lock and start one up
    synchronized {
      _worker_thread match {
        case Some(thread) => Unit // do nothing
        case None =>
          val t = initWorkerThread()
          _worker_thread = Some(t)
          t.start()
      }
    }
  }
  private def initWorkerThread(): Thread = {
    DebugLog("No worker thread; starting one up.", LogLevel.INFO, LogType.ADAPTER, null)
    new Thread(new Runnable() {
      override def run() {
        while (true) {

          val time = Stopwatch {
            _work_queue.take() match {
              case req: ShutdownReq => return
              case req: AcceptReq[_] => do_sync_action(req, () => scheduled_accept(req.t))
              case req: BudgetReq => do_sync_action(req, () => scheduled_get_budget())
              case req: CancelReq[_] => do_sync_action(req, () => scheduled_cancel(req.t))
              case req: DisposeQualsReq => do_sync_action(req, () => scheduled_cleanup_qualifications(req.q))
              case req: CreateHITReq[_] => do_sync_action(req, () => scheduled_post(req.ts, req.exclude_worker_ids))
              case req: RejectReq[_] => do_sync_action(req, () => scheduled_reject(req.t))
              case req: RetrieveReq[_] => do_sync_action(req, () => scheduled_retrieve(req.ts))
            }
          }

          // rate-limit
          Thread.sleep(Math.max(sleep_ms - time.duration_ms, 0))
        } // exit loop
      }
    })
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
  private def scheduled_accept[A](t: Thunk[A]) : Thunk[A] = {
    DebugLog(String.format("Accepting task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        backend.approveAssignment(mtq.thunk_assnid_map(t), "Thanks!")
        t.copy_as_accepted()
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  private def scheduled_cancel[A](t: Thunk[A]) : Thunk[A] = {
    DebugLog(String.format("Cancelling task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        mtq.hits.filter{_.state == HITState.RUNNING}.foreach { hit =>
          backend.forceExpireHIT(hit.hit.getHITId)
          hit.state = HITState.RESOLVED
        }
        t.copy_as_cancelled()
      }
      case _ => throw new Exception("Impossible error.")
    }
  }

  private def mturk_registerHITType(question: Question[_], worker_timeout: Int, cost: BigDecimal, quals: List[QualificationRequirement]) : HITType = {
    val hit_type_id = backend.registerHITType(
      (30 * 24 * 60 * 60).toLong,                                   // 30 days
      worker_timeout.toLong,                                        // amount of time the worker has to complete the task
      cost.toDouble,                                                // cost in USD
      question.title,                                               // title
      question.asInstanceOf[MTurkQuestion].keywords.mkString(","),  // keywords
      question.asInstanceOf[MTurkQuestion].description,             // description
      quals.toArray                                                 // no quals initially
    )
    HITType(hit_type_id, quals)
  }

  private def mturk_createHIT(ts: List[Thunk[_]], batch_key: BatchKey, question: Question[_]) : HITState = {
    // get hit_type for batch
    val hit_type = get_or_create_hittype(batch_key, question)

    val hit = backend.createHIT(
      hit_type.id,                                                  // hitTypeId
      null,                                                         // title; defined by HITType
      null,                                                         // description
      null,                                                         // keywords; defined by HITType
      question.asInstanceOf[MTurkQuestion].toXML(true).toString(),  // question
      null,                                                         // reward; defined by HITType
      null,                                                         // assignmentDurationInSeconds; defined by HITType
      null,                                                         // autoApprovalDelayInSeconds; defined by HITType
      ts.head.timeout_in_s.toLong,                                  // lifetimeInSeconds
      ts.size,                                                      // maxAssignments
      "automan",                                                    // requesterAnnotation
      Array[QualificationRequirement](),                            // qualificationRequirements; defined by HITType
      Array[String]())                                              // responseGroup
    // we immediately query the backend for the HIT's complete details
    // because the HIT structure returned by createHIT has a number
    // of uninitialized fields; return new HITState
    val hs = HITState(backend.getHIT(hit.getHITId), ts, hit_type)

    // calculate new HIT key
    val hit_key = (batch_key, question.id)

    // update HIT key -> HIT ID map
    _hit_ids = _hit_ids + (hit_key -> hs.HITId)

    // update HIT ID -> HITState map
    _hit_states = _hit_states + (hs.HITId -> hs)

    hs
  }

  private def mturk_extendHIT(ts: List[Thunk[_]], timeout_in_s: Int, hitstate: HITState) : Unit = {
    backend.extendHIT(hitstate.HITId, ts.size, timeout_in_s.toLong)
    // we immediately query the backend for the HIT's complete details
    // to update our cached data

    // update HITState and return
    val hs = hitstate.addNewThunks(backend.getHIT(hitstate.HITId), ts)

    // update hit states with new object
    _hit_states = _hit_states + (hs.HITId -> hs)

    hs
  }

  /**
   * Returns true if this group_id has never been associated with
   * any work on MTurk.
   * @param group_id The group_id.
   * @return True if the group_id has already scheduled tasks on MTurk.
   */
  private def first_run(group_id: String) : Boolean = {
    !_hit_types.map{ case (gid, _, _) => gid }.toSet.contains(group_id)
  }

  /**
   * Checks that a HITType already exists for the task group;
   * if it does, it returns the associated HITType object,
   * otherwise it creates a HITType on MTurk.
   * @param key A GroupKey tuple that uniquely identifies a batch round.
   * @param question
   * @return
   */
  private def get_or_create_hittype(key: BatchKey, question: Question[_]) : HITType = {
    // when these properties change from what we've seen before
    // (including the possibility that we've never seen any of these
    // thunks before) we need to create a new HITType;
    // Note that simply adding blacklisted/excluded workers to an existing group
    // is not sufficient to trigger the creation of a new HITType, nor do we want
    // it to, because MTurk's extendHIT is sufficient to prevent re-participation
    // for a given HIT.
    val (group_id, cost, worker_timeout) = key
    if (!_hit_types.contains(key)) {
      // whenever we create a new group, we need to add a disqualification
      // EXCEPT if it's the very first time the group is posted
      // AND we weren't specifically asked to blacklist any workers
      val quals = if (question.blacklisted_workers.size > 0 || !first_run(group_id)) {
        mturk_createDisqualification(question.asInstanceOf[MTurkQuestion], question.text, question.id) ::
          question.asInstanceOf[MTurkQuestion].qualifications
      } else {
        question.asInstanceOf[MTurkQuestion].qualifications
      }

      // request new HITTypeId from MTurk
      val hittype = mturk_registerHITType(question, worker_timeout, cost, quals)

      // update hittype map
      _hit_types = _hit_types + (key -> hittype)

      // update qualification map
      _qualifications = _qualifications ++ quals.map(_.getQualificationTypeId -> hittype.id)
    }
    _hit_types(key)
  }

  /**
   * This call marshals data to MTurk, updating local state
   * where necessary.
   * @param ts  A List of Thunks to post.
   * @param exclude_worker_ids  A list of worker_ids to exclude (via disqualifications)
   */
  private def scheduled_post(ts: List[Thunk[_]], exclude_worker_ids: List[String]) : Unit = {
    // One consequence of dealing with groups of thunks is that
    // they may each be associated with a different question; although
    // automan never calls post with heterogeneous set of thunks, we
    // have to allow for the possibility that it does.
    ts.groupBy(_.question).foreach { case (q, qts) =>
      // Our questions are *always* MTurkQuestions
      val mtq = q.asInstanceOf[MTurkQuestion]

      // also, we need to ensure that all the thunks have the same properties
      qts.groupBy{ t => (t.cost,t.worker_timeout)}.foreach { case ((cost,worker_timeout), tz) =>
        // The batch is uniquely determined by group_id, cost, and worker_timeout
        val batch_key: BatchKey = (mtq.group_id, cost, worker_timeout)

        // A HIT is uniquely determined by question_id, cost, and worker_timeout
        val hit_key: HITKey = (batch_key, q.id)

        // have we already posted a HIT for these thunks?
        if (_hit_ids.contains(hit_key)) {
          // if so, get HITState and extend it
          mturk_extendHIT(tz, tz.head.timeout_in_s, _hit_states(_hit_ids(hit_key)))
        } else {
          // if not, post a new HIT on MTurk
          mturk_createHIT(tz, batch_key, q)
        }
      }
    }
  }

  private def scheduled_reject[A](t: Thunk[A]) : Thunk[A] = {
    DebugLog(String.format("Rejecting task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        // TODO: we should tell them why
        //       the old call needed to go because distribution questions do
        //       do not come with a confidence value.
        backend.rejectAssignment(mtq.thunk_assnid_map(t), "Your answer is incorrect.")
        val t2 = t.copy_as_rejected()

        assert(t2.answer != None)
        val ans = t2.answer.get

        ???
//        if (!ans.paid) {
//          ans.memo_handle.setPaidStatus(true)
//          ans.memo_handle.save()
//          ans.paid = true
//        }
        t2
      }
      case _ => throw new Exception("Impossible error.")
    }
  }

  private def HITKey(t: Thunk[_]) : HITKey = ((t.question.asInstanceOf[MTurkQuestion].group_id, t.cost, t.worker_timeout), t.question.id)

  private def scheduled_retrieve[A](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    ts.groupBy(HITKey(_)).foreach { case (hit_key, tz) =>
      // get HITState
      val hit_state = _hit_states(_hit_ids(hit_key))

      // before we do anything, grant outstanding qualification requests for this HIT
      mturk_grantQualifications(hit_state)

      // get assignments (unmarshal)
      val assns = backend.getAllAssignmentsForHIT(hit_state.HITId).toList

      // pair thunks with available assignments & store new HITState
      val updated_hit_state = hit_state.matchAssignments(assns)
      _hit_states = _hit_states + (hit_state.HITId -> updated_hit_state)

      // update thunks and return
      ts.map { t =>
        updated_hit_state.getAssignmentOption(t) match {
          // when a Thunk is paired with an answer
          case Some(assignment) =>
            val xml = scala.xml.XML.loadString(assignment.getAnswer)
            val answer = t.question.asInstanceOf[MTurkQuestion].fromXML(xml)
            t.copy_with_answer(answer.asInstanceOf[A], assignment.getWorkerId)
          // when a Thunk is not paired with an answer
          case None => t
        }
      }

    }

    // TODO: qualification requests

    ???
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

  private def mtquestion_for_thunks(ts: List[Thunk[_]]) : MTurkQuestion = {
    // determine which MT question we've been asked about
    question_for_thunks(ts) match {
      case mtq: MTurkQuestion => mtq
      case _ => throw new Exception("MTurkAdapter can only operate on Thunks for MTurkQuestions.")
    }
  }
  private[mturk] def question_for_thunks(ts: List[Thunk[_]]) : Question[_] = {
    // determine which question we've been asked about
    val tg = ts.groupBy(_.question)
    if(tg.size != 1) {
      throw new Exception("MTurkAdapter can only process groups of Thunks for the same Question.")
    }
    tg.head._1
  }

//  private def disqualify_worker(q: MTurkQuestion, worker_id: String, question_id: UUID) : Unit = {
//    // grant dequalification Qualification
//    // AMT checks whether worker's assigned value == 1; if so, not allowed
//    if (q.worker_is_qualified(q.disqualification.getQualificationTypeId, worker_id)) {
//      // the user may have asked for the dequalification for second-round thunks
//      DebugLog("Updating worker dequalification for " + worker_id + ".", LogLevel.INFO, LogType.ADAPTER, question_id)
//      backend.updateQualificationScore(q.disqualification.getQualificationTypeId, worker_id, 1)
//    } else {
//      // otherwise, just grant it
//      DebugLog("Dequalifying worker " + worker_id + " from future work.", LogLevel.INFO, LogType.ADAPTER, question_id)
//      backend.assignQualification(q.disqualification.getQualificationTypeId, worker_id, 1, false)
//      q.qualify_worker(q.disqualification.getQualificationTypeId, worker_id)
//    }
//  }

  private def mturk_grantQualifications(hitstate: HITState) : Unit = {
    // get all requests for this HIT's group qualification
    val requests = hitstate.hittype.quals.map { qual =>
      backend.getAllQualificationRequests(qual.getQualificationTypeId)
    }.flatten

    requests.foreach { request =>
      // "SubjectId" === "WorkerId"
      val worker_id = request.getSubjectId

      // the HITType being requested
      val hit_type_id = if(_qualifications.contains(request.getQualificationTypeId)) {
        _qualifications(request.getQualificationTypeId)
      } else {
        throw new Exception("User-defined qualifications not yet supported.")
      }

      if (_worker_whitelist.contains(worker_id)) {
        if (_worker_whitelist(worker_id) != hit_type_id) {
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
        _worker_whitelist += (worker_id -> hit_type_id)
        backend.grantQualification(request.getQualificationRequestId, 0)
      }
    }
  }

  private def mark_hit_complete[A](hit: AutomanHIT, ts: List[Thunk[A]]) {
    if (ts.count{_.state == SchedulerState.RUNNING} == 0) {
      // we're done
      DebugLog("HIT is RESOLVED.", LogLevel.INFO, LogType.ADAPTER, hit.id)
      hit.state = HITState.RESOLVED
    }
  }
  private def cancel_hit[A](hit: AutomanHIT, ts: List[Thunk[A]]) : List[Thunk[A]] = {
    var hitcancelled = false
    ts.map { t =>
      if (t.is_timedout && t.state == SchedulerState.RUNNING) {
        DebugLog("HIT TIMED OUT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
        val t2 = t.copy_as_timeout()
        if (!hitcancelled) {
          DebugLog("Force-expiring HIT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
          hit.cancel(backend)
          hitcancelled = true
        }
        t2
      } else {
        t
      }
    }
  }
  private def process_assignment[A](q: MTurkQuestion, a: Assignment, hit_id: String, t: Thunk[A], use_disq: Boolean) : Thunk[A] = {
    DebugLog("Processing assignment...", LogLevel.WARN, LogType.ADAPTER, t.question.id)

    // convert answer from XML
    val answer = q.answer(a).asInstanceOf[A]

    // write custominfo
    // TODO: fix
    ???
//    answer.custom_info = Some(new MTurkAnswerCustomInfo(a.getAssignmentId, hit_id).toString)

    // new thunk
    val t2 = t.copy_with_answer(answer, a.getWorkerId)

    // disqualify worker
    if (use_disq) {
      disqualify_worker(q, a.getWorkerId, t.question.id)
    }

    // pair assignment with thunk
    q.thunk_assnid_map += (t2 -> a.getAssignmentId)  // I believe that .getAssignmentId is just a local getter

    t2
  }

  private def set_paid_status[A](t: Thunk[A]) : Unit = {
    ???
  }

  private def mturk_createDisqualification(q: MTurkQuestion, title: String, question_id: UUID) : QualificationRequirement = {
    // get a simply-formatted date
    val sdf = new SimpleDateFormat("yyyy-MM-dd:z")
    val datestr = sdf.format(new Date())

    DebugLog("Creating dequalification.",LogLevel.INFO,LogType.ADAPTER,question_id)
    val qualtxt = String.format("AutoMan automatically generated Qualification (title: %s, date: %s)", title, datestr)
    val qual : QualificationType = backend.createQualificationType("AutoMan " + UUID.randomUUID(), "automan", qualtxt)
    new QualificationRequirement(qual.getQualificationTypeId, Comparator.NotEqualTo, 1, null, false)
  }
}
