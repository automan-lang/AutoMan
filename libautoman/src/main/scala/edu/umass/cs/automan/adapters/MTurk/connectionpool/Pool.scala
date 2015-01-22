package edu.umass.cs.automan.adapters.MTurk.connectionpool

import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import com.amazonaws.mturk.requester.{Comparator, QualificationType, QualificationRequirement, Assignment}
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.MTurk.memoizer.MTurkAnswerCustomInfo
import edu.umass.cs.automan.adapters.MTurk.{AutomanHIT, MTurkAdapterNotInitialized, MTurkAdapter}
import edu.umass.cs.automan.adapters.MTurk.question._
import edu.umass.cs.automan.core.answer.{FreeTextAnswer, CheckboxAnswer, RadioButtonAnswer, Answer}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities}
import scala.collection.mutable
import scala.collection.mutable.Queue
import scala.concurrent.ExecutionContext

class Pool(backend: RequesterService, sleep_ms: Int, shutdown_delay_ms: Int) {
  // we need a thread pool more appropriate for lots of long-running I/O
  private val MAX_PARALLELISM = 1000
  private val DEFAULT_POOL = new java.util.concurrent.ForkJoinPool(MAX_PARALLELISM)
  implicit val ec = ExecutionContext.fromExecutor(DEFAULT_POOL)

  // work queues
  private val _accept_queue: Queue[AcceptReq[_ <: Answer]] = Queue.empty
  private val _cancel_queue: Queue[CancelReq[_ <: Answer]] = Queue.empty
  private val _dispose_quals_queue: Queue[DisposeQualsReq] = Queue.empty
  private val _post_queue : Queue[EnqueuedHIT[_ <: Answer]] = Queue.empty
  private val _retrieve_queue: Queue[RetrieveReq[_ <: Answer]] = Queue.empty
  private val _reject_queue: Queue[RejectReq[_ <: Answer]] = Queue.empty
  private val _timeout_queue: Queue[TimeoutReq[_ <: Answer]] = Queue.empty
  private var _worker_thread: Option[Thread] = None

  // response data
  private val _accepted_thunks = scala.collection.mutable.Map[AcceptReq[_ <: Answer], Thunk[_ <: Answer]]()
  private val _cancelled_thunks = scala.collection.mutable.Map[CancelReq[_ <: Answer], Thunk[_ <: Answer]]()
  private val _disposal_completions = scala.collection.mutable.Map[DisposeQualsReq,Unit]()
  private val _rejected_thunks = scala.collection.mutable.Map[RejectReq[_ <: Answer], Thunk[_ <: Answer]]()
  private val _retrieved_data = scala.collection.mutable.Map[RetrieveReq[_ <: Answer], List[Thunk[_ <: Answer]]]()
  private val _timeout_thunks = scala.collection.mutable.Map[TimeoutReq[_ <: Answer], List[Thunk[_ <: Answer]]]()

  def enqueue_request[M <: Message, T](req: M, to_queue: mutable.Queue[M], receive_map: mutable.Map[M,T]) = {
    // put job in queue
    synchronized {
      to_queue.enqueue(req)
    }

    initWorkerIfNeeded()
    // wait for response
    // while loop is because the JVM is
    // permitted to send spurious wakeups
    while(synchronized { !receive_map.contains(req) }) {
      // Note that the purpose of this second lock
      // is to provide blocking semantics
      req.synchronized {
        Utilities.DebugLog("Response from adapter for not available (" + req.toString + "), putting Question Scheduler to sleep.", LogLevel.INFO, LogType.ADAPTER, null)
        req.wait() // block until cancelled thunk is available
      }
    }

    // return output
    synchronized {
      val ret = receive_map(req)
      receive_map.remove(req)
      ret
    }
  }

  // API
  def accept[A <: Answer](t: Thunk[A]) : Thunk[A] = {
    enqueue_request(AcceptReq(t), _accept_queue, _accepted_thunks).asInstanceOf[Thunk[A]]
  }
  def budget(): BigDecimal = {
    // budget requests are made as soon as is possible (not queued)
    syncCommWait { () =>
      scheduled_get_budget()
    }
  }
  def cancel[A <: Answer](t: Thunk[A]) : Thunk[A] = {
    enqueue_request(CancelReq(t), _cancel_queue, _cancelled_thunks).asInstanceOf[Thunk[A]]
  }
  def cleanup_qualifications(q: Question) : Unit = {
    val req = q match {
      case mtq:MTurkQuestion => {
        enqueue_request(DisposeQualsReq(mtq), _dispose_quals_queue, _disposal_completions)
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  def post[A <: Answer](ts: List[Thunk[A]], exclude_worker_ids: List[String]) : Unit = {
    // enqueue
    synchronized {
      _post_queue.enqueue(EnqueuedHIT(ts, exclude_worker_ids))
    }

    initWorkerIfNeeded()
  }
  // put HIT's AssignmentId back into map or mark as PROCESSED
  def process_custom_info[A <: Answer](t: Thunk[A], i: Option[String]) : Thunk[A] = {
    val q = question_for_thunks(List(t))
    Utilities.DebugLog("Processing custom info...", LogLevel.INFO, LogType.ADAPTER, q.id)
    t.question match {
      case mtq: MTurkQuestion => {
        if (!t.answer.get.paid) {
          Utilities.DebugLog("Answer is not paid for; leaving thunk as RETRIEVED.", LogLevel.INFO, LogType.ADAPTER, q.id)
          val info = t.answer.get.custom_info match {
            case Some(str) => {
              val ci = new MTurkAnswerCustomInfo
              ci.parse(str)
              ci
            }
            case None => throw new Exception("Invalid memo entry.")
          }
          mtq.thunk_assnid_map += (t -> info.assignment_id)
          t
        } else {
          Utilities.DebugLog("Answer was previously paid for; marking thunk as PROCESSED.", LogLevel.INFO, LogType.ADAPTER, q.id)
          t.copy_as_processed()
        }
      }
      case _ => throw new Exception("MTurkAdapter can only operate on Thunks for MTurkQuestions.")
    }
  }
  def reject[A <: Answer](t: Thunk[A]) : Thunk[A] = {
    enqueue_request(RejectReq(t), _reject_queue, _rejected_thunks).asInstanceOf[Thunk[A]]
  }
  def retrieve[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    enqueue_request(RetrieveReq(ts), _retrieve_queue, _retrieved_data).asInstanceOf[List[Thunk[A]]]
  }
  def timeout[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    enqueue_request(TimeoutReq(ts), _timeout_queue, _timeout_thunks).asInstanceOf[List[Thunk[A]]]
  }

  private def ifWorkToBeDoneThen[T](when_yes: () => T)(when_no: () => T) : T = {
    synchronized {
      val yes =
        _post_queue.nonEmpty ||
          _cancel_queue.nonEmpty ||
          _accept_queue.nonEmpty ||
          _retrieve_queue.nonEmpty ||
          _reject_queue.nonEmpty ||
          _dispose_quals_queue.nonEmpty
      if (yes) {
        when_yes()
      } else {
        when_no()
      }
    }
  }

  private def workToBeDone: Boolean = {
    ifWorkToBeDoneThen(() => true)(() => false)
  }
  // if an item is in the queue, it is dequeued
  // f is executed inside the lock
  // g is executed outside the lock
  private def lockDequeueAndProcessAndThen[T](q: Queue[T], f: T => Unit, g: T => Unit) = {
    // Dequeue and execute f inside lock
    val topt: Option[T] = syncCommWait { () =>
      val t = if (q.nonEmpty) {
        Some(q.dequeue())
      } else {
        None
      }
      t match {
        case Some(th) => f(th); t
        case None => t
      }
    }
    // execute g outside lock, if there was an item to dequeue
    topt match {
      case Some(t) => g(t)
      case None => Unit
    }
  }
  private def lockDequeueAndProcess[T](q: Queue[T], f: T => Unit) = {
    lockDequeueAndProcessAndThen(q, f, (t: T) => Unit /* do nothing */)
  }
  private def dequeueAllAndProcess[T](q: Queue[T], f: Seq[T] => Unit) = {
    syncCommWait { () =>
      val t = synchronized {
        if (q.nonEmpty) {
          Some(q.dequeueAll(t => true))
        } else {
          None
        }
      }
      t match {
        case Some(t) => f(t)
        case None => Unit
      }
    }
  }
  // communication always syncs on 'this' adapter object
  // so this methods prevents multiple requests from
  // happening simultaneously
  private def syncCommWait[T](f: () => T) : T = {
    synchronized {
      val t = f()
      Thread.sleep(sleep_ms)
      t
    }
  }
  private def initWorkerIfNeeded() : Unit = {
    // if there's no thread already servicing the queue,
    // lock and start one up
    synchronized {
      _worker_thread match {
        case Some(thread) => Unit // do nothing
        case None =>
          val t = initConnectionPoolThread()
          _worker_thread = Some(t)
          t.start()
      }
    }
  }

  private def initConnectionPoolThread(): Thread = {
    Utilities.DebugLog("No worker thread; starting one up.", LogLevel.INFO, LogType.ADAPTER, null)
    new Thread(new Runnable() {
      override def run() {
        var keep_running = true
        while (keep_running) {
          if (workToBeDone) {
            // Post queue
            while(synchronized { _post_queue.nonEmpty } ) {
              lockDequeueAndProcess(_post_queue, (eh: EnqueuedHIT[_]) => scheduled_post(eh.ts, eh.exclude_worker_ids))
            }

            // Retrieve queue
            while (synchronized { _retrieve_queue.nonEmpty } )
              lockDequeueAndProcess(_retrieve_queue, (rr: RetrieveReq[_ <: Answer]) => {
                // this lock provides notifications
                // for blocked threads
                rr.synchronized {
                  // do request
                  _retrieved_data += (rr -> scheduled_retrieve(rr.ts))

                  // send end-wait notification
                  rr.notifyAll()
                }
              })

            // Timeout queue
            while (synchronized { _timeout_queue.nonEmpty } )
              lockDequeueAndProcess(_timeout_queue, (tr: TimeoutReq[_ <: Answer]) => {
                // this lock provides notifications
                // for blocked threads
                tr.synchronized {
                  // do request
                  _timeout_thunks += (tr -> scheduled_timeout(tr.ts))

                  // send end-wait notification
                  tr.notifyAll()
                }
              })

            // Accept queue
            while (synchronized { _accept_queue.nonEmpty } ) {
              lockDequeueAndProcess(_accept_queue, (ar: AcceptReq[_ <: Answer]) => {
                // this lock provides notifications
                // for blocked threads
                ar.synchronized {
                  // do request
                  val accepted_thunk = scheduled_accept(ar.t)

                  // set paid status in answer
                  set_paid_status(accepted_thunk)

                  // store response thunk
                  _accepted_thunks += (ar -> accepted_thunk)

                  // send end-wait notification
                  ar.notifyAll()
                }
              })
            }

            // Reject queue
            while (synchronized { _reject_queue.nonEmpty } ) {
              lockDequeueAndProcess(_reject_queue, (rj: RejectReq[_ <: Answer]) => {
                // this lock provides notifications
                // for blocked threads
                rj.synchronized {
                  // do request
                  _rejected_thunks += (rj -> scheduled_reject(rj.t))

                  // send end-wait notification
                  rj.notifyAll()
                }
              })
            }

            // Cancel queue
            while (synchronized { _cancel_queue.nonEmpty } ) {
              lockDequeueAndProcess(_cancel_queue, (cr: CancelReq[_ <: Answer]) => {
                // this lock provides notifications
                // for blocked threads
                cr.synchronized {
                  // do request
                  _cancelled_thunks += (cr -> scheduled_cancel(cr.t))

                  // send end-wait notification
                  cr.notifyAll()
                }
              })
            }

            // Cleanup qualifications queue
            while (synchronized { _dispose_quals_queue.nonEmpty }) {
              lockDequeueAndProcess(_dispose_quals_queue, (qr: DisposeQualsReq) => {
                // this lock provides notifications
                // for blocked threads
                qr.synchronized {
                  // do request
                  scheduled_cleanup_qualifications(qr.q)

                  // send end-wait notification
                  _disposal_completions += (qr -> Unit)
                  qr.notifyAll()
                }
              })
            }

          } else {
            // sleep a bit to avoid unnecessary thread startup churn
            Utilities.DebugLog("No work remains; sleeping.", LogLevel.INFO, LogType.ADAPTER, null)
            Thread.sleep(shutdown_delay_ms)
            // if it's still empty, break
            ifWorkToBeDoneThen (() => Unit /* keep running */)(() => {
              Utilities.DebugLog("No work remains after sleep; shutting down thread.", LogLevel.INFO, LogType.ADAPTER, null)
              keep_running = false
              _worker_thread = None
              Unit
            })
          }
        } // exit loop
      }
    })
  }

  private def scheduled_accept[A <: Answer](t: Thunk[A]) : Thunk[A] = {
    Utilities.DebugLog(String.format("Accepting task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        backend.approveAssignment(mtq.thunk_assnid_map(t), "Thanks!")
        t.copy_as_accepted()
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  private def scheduled_cancel[A <: Answer](t: Thunk[A]) : Thunk[A] = {
    Utilities.DebugLog(String.format("Cancelling task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

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
    Utilities.DebugLog(String.format("Posting %s tasks.", ts.size.toString()), LogLevel.INFO, LogType.ADAPTER, null)

    val question = question_for_thunks(ts)
    val mtquestion = question match { case mtq: MTurkQuestion => mtq; case _ => throw new Exception("Impossible.") }
    val qualify_early = if (question.blacklisted_workers.size > 0) true else false
    val quals = get_qualifications(mtquestion, ts.head.question.text, qualify_early, question.id, question.use_disqualifications)

    // Build HIT and post it
    mtquestion match {
      case rbq: MTRadioButtonQuestion => {
        mtquestion.hit_type_id = rbq.build_hit(ts).post(backend, quals)
      }
      case rbdq: MTRadioButtonDistributionQuestion => {
        mtquestion.hit_type_id = rbdq.build_hit(ts).post(backend, quals)
      }
      case cbq: MTCheckboxQuestion => {
        mtquestion.hit_type_id = cbq.build_hit(ts).post(backend, quals)
      }
      case cbdq: MTCheckboxDistributionQuestion => {
        mtquestion.hit_type_id = cbdq.build_hit(ts).post(backend, quals)
      }
      case ftq: MTFreeTextQuestion => {
        mtquestion.hit_type_id = ftq.build_hit(ts).post(backend, quals)
      }
      case ftdq: MTFreeTextDistributionQuestion => {
        mtquestion.hit_type_id = ftdq.build_hit(ts).post(backend, quals)
      }
      case _ => throw new Exception("Question type not yet supported. Question class is " + mtquestion.getClass)
    }
  }
  private def scheduled_reject[A <: Answer](t: Thunk[A]) : Thunk[A] = {
    Utilities.DebugLog(String.format("Rejecting task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        // TODO: we should tell them why
        //       the old call needed to go because distribution questions do
        //       do not come with a confidence value.
        backend.rejectAssignment(mtq.thunk_assnid_map(t), "Your answer is incorrect.")
        val t2 = t.copy_as_rejected()

        assert(t2.answer != None)
        val ans = t2.answer.get

        if (!ans.paid) {
          ans.memo_handle.setPaidStatus(true)
          ans.memo_handle.save()
          ans.paid = true
        }
        t2
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  private def scheduled_retrieve[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    Utilities.DebugLog(String.format("Retrieving %s tasks.", ts.size.toString()), LogLevel.INFO, LogType.ADAPTER, null)

    val question = mtquestion_for_thunks(ts)
    val auquestion = question_for_thunks(ts)
    val hits = question.hits.filter{_.state == HITState.RUNNING}

    // start by granting qualifications
    grant_qualification_requests(question, auquestion.blacklisted_workers, auquestion.id)

    // eagerly grab assignments for every known HIT
    val ts2 = hits.map { hit =>
      // for each HIT, ensure that we use the correct thunk
      // by looking in the hit_thunk_map
      val hts = Queue[Thunk[A]]()
      hts ++= question.hit_thunk_map(hit).filter(ts.contains(_)).asInstanceOf[List[Thunk[A]]]
      val assignments: List[Assignment] = hit.retrieve(backend) // finally, do MTurk call

      Utilities.DebugLog("There are " + assignments.size + " assignments available to process.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)

      // for every available thunk-answer pairing, mark thunk as RETRIEVED
      val answered_ts = assignments.map { a => process_assignment(question, a, hit.hit.getHITId, hts.dequeue(), auquestion.use_disqualifications) }

      // return updated thunks (both answered and unanswered)
      answered_ts ::: hts.toList
    }.flatten

    Utilities.DebugLog(ts2.count{_.state == SchedulerState.RETRIEVED} + " thunks marked RETRIEVED.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)
    ts2.filterNot(_.state == SchedulerState.RUNNING)
  }
  private def scheduled_timeout[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    if(ts.size == 0) {
      ts
    } else {
      Utilities.DebugLog(String.format("Checking %s tasks for TIMEOUT.", ts.size.toString()), LogLevel.INFO, LogType.ADAPTER, null)

      val question = mtquestion_for_thunks(ts)
      val auquestion = question_for_thunks(ts)
      val hits = question.hits.filter{_.state == HITState.RUNNING}

      val ts2 = hits.map { hit =>
        // for each HIT, ensure that we use the correct thunk
        // by looking in the hit_thunk_map
        val hts = Queue[Thunk[A]]()
        hts ++= question.hit_thunk_map(hit).filter(ts.contains(_)).asInstanceOf[List[Thunk[A]]]

        // timeout timed out Thunks and the HIT
        // since hts is a queue, and we dequeued answered thunks in
        // in the previous call, hts does not include answered thunks
        val timeout_processed_thunks = process_timeouts(hit, hts.toList)

        // check to see if we need to continue running this HIT
        mark_hit_complete(hit, timeout_processed_thunks)

        // return all thunks
        timeout_processed_thunks
      }.flatten

      Utilities.DebugLog(ts2.count{_.state == SchedulerState.TIMEOUT} + " thunks marked TIMEOUT.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)
      ts2.filterNot(_.state == SchedulerState.RUNNING)
    }
  }
  private def scheduled_get_budget(): BigDecimal = {
    Utilities.DebugLog("Getting budget.", LogLevel.INFO, LogType.ADAPTER, null)
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
  private[MTurk] def question_for_thunks(ts: List[Thunk[_]]) : Question = {
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
      Utilities.DebugLog("Updating worker dequalification for " + worker_id + ".", LogLevel.INFO, LogType.ADAPTER, question_id)
      backend.updateQualificationScore(q.disqualification.getQualificationTypeId, worker_id, 1)
    } else {
      // otherwise, just grant it
      Utilities.DebugLog("Dequalifying worker " + worker_id + " from future work.", LogLevel.INFO, LogType.ADAPTER, question_id)
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
        Utilities.DebugLog("Worker " + qr.getSubjectId + " cannot request a qualification for a question that they are blacklisted for; rejecting request.", LogLevel.INFO, LogType.ADAPTER, question_id)
        backend.rejectQualificationRequest(qr.getQualificationRequestId, "You may not work on this particular hit for statistical purposes.")
      } else if (q.worker_is_qualified(qr.getQualificationTypeId, qr.getSubjectId)) {
        // we don't want to grant more than once
        Utilities.DebugLog("Worker " + qr.getSubjectId + " cannot request a qualification more than once; rejecting request.", LogLevel.INFO, LogType.ADAPTER, question_id)
        backend.rejectQualificationRequest(qr.getQualificationRequestId, "You cannot request this Qualification more than once.")
      } else {
        // grant
        if (qr.getQualificationTypeId == q.disqualification.getQualificationTypeId) {
          Utilities.DebugLog("Worker " + qr.getSubjectId + " requests one-time qualification; granting.", LogLevel.INFO, LogType.ADAPTER, question_id)
          backend.grantQualification(qr.getQualificationRequestId, 0)
          q.qualify_worker(qr.getQualificationTypeId, qr.getSubjectId)
        } else {
          throw new Exception("User-defined qualifications not yet supported.")
        }
      }
    }
  }
  private def mark_hit_complete[A <: Answer](hit: AutomanHIT, ts: List[Thunk[A]]) {
    if (ts.count{_.state == SchedulerState.RUNNING} == 0) {
      // we're done
      Utilities.DebugLog("HIT is RESOLVED.", LogLevel.INFO, LogType.ADAPTER, hit.id)
      hit.state = HITState.RESOLVED
    }
  }
  private def process_timeouts[A <: Answer](hit: AutomanHIT, ts: List[Thunk[A]]) : List[Thunk[A]] = {
    var hitcancelled = false
    ts.map { t =>
      if (t.is_timedout) {
        Utilities.DebugLog("HIT TIMED OUT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
        val t2 = t.copy_as_timeout()
        if (!hitcancelled) {
          Utilities.DebugLog("Force-expiring HIT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
          hit.cancel(backend)
          hitcancelled = true
        }
        t2
      } else {
        t
      }
    }
  }
  private def process_assignment[A <: Answer](q: MTurkQuestion, a: Assignment, hit_id: String, t: Thunk[A], use_disq: Boolean) : Thunk[A] = {
    Utilities.DebugLog("Processing assignment...", LogLevel.WARN, LogType.ADAPTER, t.question.id)

    // convert answer from XML
    val answer = q.answer(a).asInstanceOf[A]

    // write custominfo
    answer.custom_info = Some(new MTurkAnswerCustomInfo(a.getAssignmentId, hit_id).toString)

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

  private def set_paid_status[A <: Answer](t: Thunk[A]) : Unit = {
    t.question match {
      case mtq:MTurkQuestion => {
        assert(t.answer != None)
        val ans = t.answer.get
        if (!ans.paid) {
          ans.memo_handle.setPaidStatus(true)
          ans.memo_handle.save()
          ans.paid = true
        }
      }
      case _ => throw new Exception("Impossible error.")
    }
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

        Utilities.DebugLog("This is the task's first run; creating dequalification.",LogLevel.INFO,LogType.ADAPTER,question_id)
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
