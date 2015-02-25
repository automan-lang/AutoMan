package edu.umass.cs.automan.adapters.mturk.connectionpool

import java.text.SimpleDateFormat
import java.util.concurrent.PriorityBlockingQueue
import java.util.{Date, UUID}
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.AutomanHIT
import edu.umass.cs.automan.adapters.mturk.question.{MTRadioButtonQuestion, HITState, MTurkQuestion}
import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.util.Stopwatch
import scala.collection.mutable.Queue

class Pool(backend: RequesterService, sleep_ms: Int) {
  // worker
  private var _worker_thread: Option[Thread] = None

  // work queue
  private val _work_queue: PriorityBlockingQueue[Message] = new PriorityBlockingQueue[Message]()

  // response data
  private val _responses = scala.collection.mutable.Map[Message, Any]()

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
  private def scheduled_post(ts: List[Thunk[_]], exclude_worker_ids: List[String]) : Unit = {
    DebugLog(String.format("Posting %s tasks.", ts.size.toString), LogLevel.INFO, LogType.ADAPTER, null)

    val question = question_for_thunks(ts)
    val mtquestion = question match { case mtq: MTurkQuestion => mtq; case _ => throw new Exception("Impossible.") }
    val qualify_early = if (question.blacklisted_workers.size > 0) true else false
    val quals = get_qualifications(mtquestion, ts.head.question.text, qualify_early, question.id, ???)  // TODO: use_disqualifications is now gone

    // Build HIT and post it
    mtquestion match {
      case rbq: MTRadioButtonQuestion => {
        mtquestion.hit_type_id = rbq.build_hit(ts).post(backend, quals)
      }
      case _ => ???
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
  private def scheduled_retrieve[A](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    DebugLog(String.format("Retrieving %s tasks.", ts.size.toString()), LogLevel.INFO, LogType.ADAPTER, null)
    val question: MTurkQuestion = mtquestion_for_thunks(ts)
    val auquestion = question_for_thunks(ts)
    val hits = question.hits.filter(_.state == HITState.RUNNING)
    // start by granting qualifications
    grant_qualification_requests(question, auquestion.blacklisted_workers, auquestion.id)
    // eagerly grab assignments for every known HIT
    val ts2 = hits.map { hit =>
      // for each HIT, ensure that we use the correct thunk
      // by looking in the hit_thunk_map
      val hts = Queue[Thunk[A]]()
      hts ++= question.hit_thunk_map(hit).filter(ts.contains(_)).asInstanceOf[List[Thunk[A]]]
      val assignments: List[Assignment] = hit.retrieve(backend) // finally, do MTurk call
      DebugLog("There are " + assignments.size + " assignments available to process.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)
      // for every available thunk-answer pairing, mark thunk as RETRIEVED
      val answered_ts = assignments.map { a => process_assignment(question, a, hit.hit.getHITId, hts.dequeue(), ???) } // TODO: use_disqualifications is now gone
      // timeout timed out Thunks and the HIT
      // since hts is a queue, and we dequeued answered thunks in
      // in the previous call, hts does not include answered thunks
      val unanswered_ts = cancel_hit(hit, hts.toList)
      // check to see if we need to continue running this HIT
      mark_hit_complete(hit, unanswered_ts)
      // return updated thunks (both answered and unanswered)
      answered_ts ::: unanswered_ts
    }.flatten
    DebugLog(ts2.count{_.state == SchedulerState.RETRIEVED} + " thunks marked RETRIEVED.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)
    ts2.filterNot(_.state == SchedulerState.RUNNING)
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

  private def disqualify_worker(q: MTurkQuestion, worker_id: String, question_id: UUID) : Unit = {
    // grant dequalification Qualification
    // AMT checks whether worker's assigned value == 1; if so, not allowed
    if (q.worker_is_qualified(q.disqualification.getQualificationTypeId, worker_id)) {
      // the user may have asked for the dequalification for second-round thunks
      DebugLog("Updating worker dequalification for " + worker_id + ".", LogLevel.INFO, LogType.ADAPTER, question_id)
      backend.updateQualificationScore(q.disqualification.getQualificationTypeId, worker_id, 1)
    } else {
      // otherwise, just grant it
      DebugLog("Dequalifying worker " + worker_id + " from future work.", LogLevel.INFO, LogType.ADAPTER, question_id)
      backend.assignQualification(q.disqualification.getQualificationTypeId, worker_id, 1, false)
      q.qualify_worker(q.disqualification.getQualificationTypeId, worker_id)
    }
  }
  private def grant_qualification_requests(q: MTurkQuestion, blacklisted_workers: List[String], question_id: UUID) : Unit = {
    // get all requests for all qualifications on this HIT
    val qrs = q.qualifications.map { qual =>
      backend.getAllQualificationRequests(qual.getQualificationTypeId)
    }.flatten

    qrs.foreach { qr =>
      if (blacklisted_workers.contains(qr.getSubjectId)) {
        // we don't want blacklisted workers working on this
        DebugLog("Worker " + qr.getSubjectId + " cannot request a qualification for a question that they are blacklisted for; rejecting request.", LogLevel.INFO, LogType.ADAPTER, question_id)
        backend.rejectQualificationRequest(qr.getQualificationRequestId, "You may not work on this particular hit for statistical purposes.")
      } else if (q.worker_is_qualified(qr.getQualificationTypeId, qr.getSubjectId)) {
        // we don't want to grant more than once
        DebugLog("Worker " + qr.getSubjectId + " cannot request a qualification more than once; rejecting request.", LogLevel.INFO, LogType.ADAPTER, question_id)
        backend.rejectQualificationRequest(qr.getQualificationRequestId, "You cannot request this Qualification more than once.")
      } else {
        // grant
        if (qr.getQualificationTypeId == q.disqualification.getQualificationTypeId) {
          DebugLog("Worker " + qr.getSubjectId + " requests one-time qualification; granting.", LogLevel.INFO, LogType.ADAPTER, question_id)
          backend.grantQualification(qr.getQualificationRequestId, 0)
          q.qualify_worker(qr.getQualificationTypeId, qr.getSubjectId)
        } else {
          throw new Exception("User-defined qualifications not yet supported.")
        }
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
  private def get_qualifications(q: MTurkQuestion, title: String, qualify_early: Boolean, question_id: UUID, use_disq: Boolean) : List[QualificationRequirement] = {
    // if we are not using the disqualification mechanism,
    // just return the user-specified list of qualifications
    if (use_disq) {
      // The first qualification always needs to be the special
      // "dequalification" type so that we may grant it as soon as
      // a worker completes some work.
      if (q.firstrun) {
        // get a simply-formatted date
        val sdf = new SimpleDateFormat("yyyy-MM-dd:z")
        val datestr = sdf.format(new Date())

        DebugLog("This is the task's first run; creating dequalification.",LogLevel.INFO,LogType.ADAPTER,question_id)
        val qualtxt = String.format("AutoMan automatically generated Qualification (title: %s, date: %s)", title, datestr)
        val qual : QualificationType = backend.createQualificationType("AutoMan " + UUID.randomUUID(), "automan", qualtxt)
        val deq = new QualificationRequirement(qual.getQualificationTypeId, Comparator.NotEqualTo, 1, null, false)
        q.disqualification = deq
        q.firstrun = false
        // we need early qualifications; add anyway
        if (qualify_early) {
          q.qualifications = deq :: q.qualifications
        }
      } else if (!q.qualifications.contains(q.disqualification)) {
        // add the dequalification to the list of quals if this
        // isn't a first run and it isn't already there
        q.qualifications = q.disqualification :: q.qualifications
      }
    } else if (q.firstrun) {
      q.firstrun = false // in case this gets used anywhere else
    }

    q.qualifications
  }
}
