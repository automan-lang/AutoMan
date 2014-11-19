package edu.umass.cs.automan.adapters.MTurk

import java.text.SimpleDateFormat

import com.amazonaws.mturk.util.ClientConfig
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.core.question._
import scala.concurrent._
import edu.umass.cs.automan.core.strategy.{ValidationStrategy, ScalarValidationStrategy}
import memoizer.MTurkAnswerCustomInfo
import question._
import edu.umass.cs.automan.core.scheduler.{Scheduler, SchedulerState, Thunk}
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities, retry, AutomanAdapter}
import java.util.{UUID, Date}
import com.amazonaws.mturk.requester._
import scala.Predef._
import scala.collection.mutable.Queue
import edu.umass.cs.automan.core.answer._

object MTurkAdapter {
  def apply(init: MTurkAdapter => Unit) : MTurkAdapter = {
    val mta = new MTurkAdapter
    init(mta)
    mta.memo_init()               // starts up Memoizer
    mta.thunklog_init()           // start up Thunk Logger
    mta.setup()                   // initializes MTurk SDK
    mta.get_budget_from_backend() // asks MTurk for our current budget
    mta
  }
}

class MTurkAdapter extends AutomanAdapter {
  // concretize types for MTurk
  override type RBQ = MTRadioButtonQuestion
  override type RBDQ = MTRadioButtonDistributionQuestion
  override type CBQ = MTCheckboxQuestion
  override type FTQ = MTFreeTextQuestion

  // we need a thread pool more appropriate for lots of long-running I/O
  private val MAX_PARALLELISM = 1000
  private val DEFAULT_POOL = new java.util.concurrent.ForkJoinPool(MAX_PARALLELISM)
  implicit val ec = ExecutionContext.fromExecutor(DEFAULT_POOL)

  private val SLEEP_MS = 500
  private val SHUTDOWN_DELAY_MS = SLEEP_MS * 10
  private case class EnqueuedHIT[A <: Answer](ts: List[Thunk[A]], dual: Boolean, exclude_worker_ids: List[String])
  private case class RetrieveReq[A <: Answer](ts: List[Thunk[A]])
  private case class BudgetReq()
  private case class DisposeQualsReq(q: MTurkQuestion)

  private var _access_key_id: Option[String] = None
  private var _poll_interval_in_s : Int = 30
  private var _retriable_errors = Set("Server.ServiceUnavailable")
  private var _retry_attempts : Int = 10
  private var _retry_delay_millis : Int = 1000
  private var _secret_access_key: Option[String] = None
  private var _service_url : String = ClientConfig.SANDBOX_SERVICE_URL
  private var _service : Option[RequesterService] = None

  // work queues
  private var _worker_thread: Option[Thread] = None
  private val _post_queue : Queue[EnqueuedHIT[_ <: Answer]] = new Queue[EnqueuedHIT[_ <: Answer]]()
  private val _cancel_queue: Queue[Thunk[_ <: Answer]] = new Queue[Thunk[_ <: Answer]]()
  private val _accept_queue: Queue[Thunk[_ <: Answer]] = new Queue[Thunk[_ <: Answer]]()
  private val _retrieve_queue: Queue[RetrieveReq[_ <: Answer]] = new Queue[RetrieveReq[_ <: Answer]]()
  private val _reject_queue: Queue[Thunk[_ <: Answer]] = new Queue[Thunk[_ <: Answer]]()
  private val _dispose_quals_queue: Queue[DisposeQualsReq] = new Queue[DisposeQualsReq]()

  // response data
  private val _retrieved_data = scala.collection.mutable.Map[RetrieveReq[_ <: Answer], List[Thunk[_ <: Answer]]]()
  private val _disposal_completions = scala.collection.mutable.Set[DisposeQualsReq]()

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
  // happening simultaneous
  private def syncCommWait[T](f: () => T) : T = {
    synchronized {
      val t = f()
      Thread.sleep(SLEEP_MS)
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
              lockDequeueAndProcess(_post_queue, (eh: EnqueuedHIT[_]) => scheduled_post(eh.ts, eh.dual, eh.exclude_worker_ids))
            }

            // Retrieve queue
            while (synchronized { _retrieve_queue.nonEmpty } )
            lockDequeueAndProcess(_retrieve_queue, (rr: RetrieveReq[_ <: Answer]) => {
              rr.synchronized {
                // do request
                _retrieved_data += (rr -> scheduled_retrieve(rr.ts))

                // send end-wait notification
                rr.notifyAll()
              }
            })

            // Approve queue
            while(synchronized { _accept_queue.nonEmpty } ) {
              lockDequeueAndProcessAndThen(_accept_queue,
                (t: Thunk[_ <: Answer]) => scheduled_accept(t), // synchronized
                (t: Thunk[_ <: Answer]) => set_paid_status(t)   // not synchronized
              )
            }

            // Reject queue
            while (synchronized { _reject_queue.nonEmpty }) {
              lockDequeueAndProcess(_reject_queue, (t: Thunk[_ <: Answer]) => scheduled_reject(t))
            }


            // Cancel queue
            while (synchronized { _cancel_queue.nonEmpty }) {
              lockDequeueAndProcess(_cancel_queue, (t: Thunk[_ <: Answer]) => scheduled_cancel(t))
            }

            // Cleanup qualifications queue
            while (synchronized { _dispose_quals_queue.nonEmpty }) {
              lockDequeueAndProcess(_dispose_quals_queue, (qr: DisposeQualsReq) => {
                qr.synchronized {
                  // do request
                  scheduled_cleanup_qualifications(qr.q)

                  // send end-wait notification
                  _disposal_completions += qr
                  qr.notifyAll()
                }
              })
            }

          } else {
            // sleep a bit to avoid unnecessary thread startup churn
            Utilities.DebugLog("No work remains; sleeping.", LogLevel.INFO, LogType.ADAPTER, null)
            Thread.sleep(SHUTDOWN_DELAY_MS)
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

  def CheckboxQuestion(q: MTCheckboxQuestion => Unit) = MTCheckboxQuestion(q, this)
  def FreeTextQuestion(q: MTFreeTextQuestion => Unit) = MTFreeTextQuestion(q, this)
  def RadioButtonQuestion(q: MTRadioButtonQuestion => Unit) = MTRadioButtonQuestion(q, this)
  def RadioButtonDistributionQuestion(q: MTRadioButtonDistributionQuestion => Unit) = MTRadioButtonDistributionQuestion(q, this)
  def Option(id: Symbol, text: String) = new MTQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new MTQuestionOption(id, text, image_url)
  def schedule(q: MTRadioButtonQuestion): Future[RadioButtonAnswer] = Future {
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    sched.run()
  }.asInstanceOf[Future[RadioButtonAnswer]]
  def schedule(q: MTRadioButtonDistributionQuestion): Future[Set[RadioButtonAnswer]] = Future {
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    sched.run()
  }.asInstanceOf[Future[Set[RadioButtonAnswer]]]
  def schedule(q: MTCheckboxQuestion): Future[CheckboxAnswer] = Future {
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    sched.run()
  }.asInstanceOf[Future[CheckboxAnswer]]
  def schedule(q: MTFreeTextQuestion): Future[FreeTextAnswer] = Future {
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    sched.run()
  }.asInstanceOf[Future[FreeTextAnswer]]

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

  def post[A <: Answer](ts: List[Thunk[A]], dual: Boolean, exclude_worker_ids: List[String]) : Unit = {
    // enqueue
    synchronized {
      _post_queue.enqueue(EnqueuedHIT(ts, dual, exclude_worker_ids))
    }

    initWorkerIfNeeded()

    // mark thunks as RUNNING so that the scheduler
    // knows to attempt to retrieve their answers later
    ts.foreach { _.state = SchedulerState.RUNNING }
  }

  private def scheduled_post(ts: List[Thunk[_]], dual: Boolean, exclude_worker_ids: List[String]) : Unit = {
    Utilities.DebugLog(String.format("Posting %s tasks.", ts.size.toString()), LogLevel.INFO, LogType.ADAPTER, null)

    val question = question_for_thunks(ts)
    val mtquestion = question match { case mtq: MTurkQuestion => mtq; case _ => throw new Exception("Impossible.") }
    val qualify_early = if (question.blacklisted_workers.size > 0) true else false
    val quals = get_qualifications(mtquestion, ts.head.question.text, qualify_early, question.id, question.use_disqualifications)

    // Build HIT and post it
    mtquestion match {
      case rbq: MTRadioButtonQuestion => {
        if (!dual) {
          mtquestion.hit_type_id = rbq.build_hit(ts, false).post(backend, quals)
        }
      }
      case rbdq: MTRadioButtonDistributionQuestion => {
        if (!dual) {
          mtquestion.hit_type_id = rbdq.build_hit(ts, false).post(backend, quals)
        }
      }
      case cbq: MTCheckboxQuestion => {
        mtquestion.hit_type_id = cbq.build_hit(ts, dual).post(backend, quals)
      }
      case ftq: MTFreeTextQuestion => {
        mtquestion.hit_type_id = ftq.build_hit(ts, false).post(backend, quals)
      }
      case _ => throw new Exception("Question type not yet supported. Question class is " + mtquestion.getClass)
    }
  }
  def mtquestion_for_thunks(ts: List[Thunk[_]]) : MTurkQuestion = {
    // determine which MT question we've been asked about
    question_for_thunks(ts) match {
      case mtq: MTurkQuestion => mtq
      case _ => throw new Exception("MTurkAdapter can only operate on Thunks for MTurkQuestions.")
    }
  }
  def question_for_thunks(ts: List[Thunk[_]]) : Question = {
    // determine which question we've been asked about
    val tg = ts.groupBy(_.question)
    if(tg.size != 1) {
      throw new Exception("MTurkAdapter can only process groups of Thunks for the same Question.")
    }
    tg.head._1
  }
  // put HIT's AssignmentId back into map or mark as PROCESSED
  def process_custom_info[A <: Answer](t: Thunk[A], i: Option[String]) {
    val q = question_for_thunks(List(t))
    Utilities.DebugLog("Processing custom info...", LogLevel.INFO, LogType.ADAPTER, q.id)
    t.question match {
      case mtq: MTurkQuestion => {
        if (!t.answer.paid) {
          Utilities.DebugLog("Answer is not paid for; leaving thunk as RETRIEVED.", LogLevel.INFO, LogType.ADAPTER, q.id)
          val info = t.answer.custom_info match {
            case Some(str) => {
              val ci = new MTurkAnswerCustomInfo
              ci.parse(str)
              ci
            }
            case None => throw new Exception("Invalid memo entry.")
          }
          mtq.thunk_assnid_map += (t -> info.assignment_id)
        } else {
          Utilities.DebugLog("Answer was previously paid for; marking thunk as PROCESSED.", LogLevel.INFO, LogType.ADAPTER, q.id)
          t.state = SchedulerState.PROCESSED
        }
      }
      case _ => throw new Exception("MTurkAdapter can only operate on Thunks for MTurkQuestions.")
    }
  }
  def cancel[A <: Answer](t: Thunk[A]) : Unit = {
    // enqueue
    synchronized {
      _cancel_queue.enqueue(t)
    }

    initWorkerIfNeeded()
  }
  private def scheduled_cancel[A <: Answer](t: Thunk[A]) : Unit = {
    Utilities.DebugLog(String.format("Cancelling task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        mtq.hits.filter{_.state == HITState.RUNNING}.foreach { hit =>
          backend.forceExpireHIT(hit.hit.getHITId)
          hit.state = HITState.RESOLVED
        }
        t.state = SchedulerState.REJECTED
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  def accept[A <: Answer](t: Thunk[A]) : Unit = {
    // enqueue
    synchronized {
      _accept_queue.enqueue(t)
    }

    initWorkerIfNeeded()
  }
  private def scheduled_accept[A <: Answer](t: Thunk[A]) : Unit = {
    Utilities.DebugLog(String.format("Accepting task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        backend.approveAssignment(mtq.thunk_assnid_map(t), "Thanks!")
        t.state = SchedulerState.ACCEPTED
        // TODO: urgh, worker_id
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  private def set_paid_status[A <: Answer](t: Thunk[A]) : Unit = {
    t.question match {
      case mtq:MTurkQuestion => {
        t.answer match {
          case rba: RadioButtonAnswer => if (!rba.paid) {
            rba.memo_handle.setPaidStatus(true)
            rba.memo_handle.save()
            rba.paid = true
          }
          case cba: CheckboxAnswer => if (!cba.paid) {
            cba.memo_handle.setPaidStatus(true)
            cba.memo_handle.save()
            cba.paid = true
          }
          case fta: FreeTextAnswer => if (!fta.paid) {
            fta.memo_handle.setPaidStatus(true)
            fta.memo_handle.save()
            fta.paid = true
          }
        }
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  def reject[A <: Answer](t: Thunk[A]) : Unit = {
    // enqueue
    synchronized {
      _reject_queue.enqueue(t)
    }

    initWorkerIfNeeded()
  }
  private def scheduled_reject[A <: Answer](t: Thunk[A]) : Unit = {
    Utilities.DebugLog(String.format("Rejecting task for question_id = %s", t.question.id), LogLevel.INFO, LogType.ADAPTER, null)

    t.question match {
      case mtq:MTurkQuestion => {
        backend.rejectAssignment(mtq.thunk_assnid_map(t), "Your answer is incorrect with a probability >" + confidence + ".")
        t.state = SchedulerState.REJECTED
        t.answer match {
          case rba: RadioButtonAnswer => if (!rba.paid) {
            rba.memo_handle.setPaidStatus(true)
            rba.memo_handle.save()
            rba.paid = true
          }
          case cba: CheckboxAnswer => if (!cba.paid) {
            cba.memo_handle.setPaidStatus(true)
            cba.memo_handle.save()
            cba.paid = true
          }
          case fta: FreeTextAnswer => if (!fta.paid) {
            fta.memo_handle.setPaidStatus(true)
            fta.memo_handle.save()
            fta.paid = true
          }
        }
      }
      case _ => throw new Exception("Impossible error.")
    }
  }

  def retrieve[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    val req = RetrieveReq(ts)

    // enqueue
    synchronized {
      _retrieve_queue.enqueue(req)
    }

    initWorkerIfNeeded()

    // wait for response
    // while loop is because the JVM is
    // permitted to send spurious wakeups
    while(synchronized { !_retrieved_data.contains(req) }) {
      req.synchronized {
        Utilities.DebugLog("Answer from Adapter not available, putting Question Scheduler to sleep.", LogLevel.INFO, LogType.ADAPTER, null)
        req.wait() // block until answer is available
      }
    }
    // return response
    synchronized {
      val response = _retrieved_data(req).asInstanceOf[List[Thunk[A]]]
      _retrieved_data.remove(req) // remove response data
      response
    }
  }

  def scheduled_retrieve[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]] = {
    Utilities.DebugLog(String.format("Retrieving %s tasks.", ts.size.toString()), LogLevel.INFO, LogType.ADAPTER, null)

    val question = mtquestion_for_thunks(ts)
    val auquestion = question_for_thunks(ts)
    val hits = question.hits.filter{_.state == HITState.RUNNING}

    // start by granting qualifications
    grant_qualification_requests(question, auquestion.blacklisted_workers, auquestion.id)

    // try grabbing something from each HIT
    hits.foreach { hit =>
      // get running thunks for each HIT
      val hts = Queue[Thunk[A]]()
      hts ++= question.hit_thunk_map(hit).filter{_.state == SchedulerState.RUNNING}.asInstanceOf[List[Thunk[A]]]
      val assignments = hit.retrieve(backend)

      Utilities.DebugLog("There are " + assignments.size + " assignments available to process.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)

      // mark next available thunk as RETRIEVED for each answer
      assignments.foreach { a => process_assignment(question, a, hit.hit.getHITId, hts.dequeue(), auquestion.use_disqualifications) }

      // timeout timed out Thunks and the HIT
      process_timeouts(hit, hts.toList)

      // check to see if we need to continue running this HIT
      mark_hit_complete(hit, hts.toList)
    }

    Utilities.DebugLog(ts.filter{_.state == SchedulerState.RETRIEVED}.size + " thunks marked RETRIEVED.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)
    ts
  }

  def mark_hit_complete[A <: Answer](hit: AutomanHIT, ts: List[Thunk[A]]) {
    if (ts.filter{_.state == SchedulerState.RUNNING}.size == 0) {
      // we're done
      Utilities.DebugLog("HIT is RESOLVED.", LogLevel.INFO, LogType.ADAPTER, hit.id)
      hit.state = HITState.RESOLVED
    }
  }
  private def process_timeouts[A <: Answer](hit: AutomanHIT, ts: List[Thunk[A]]) = {
    var hitcancelled = false
    ts.filter{_.is_timedout}.foreach { t =>
      Utilities.DebugLog("HIT TIMED OUT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
      t.state = SchedulerState.TIMEOUT
      if (!hitcancelled) {
        Utilities.DebugLog("Force-expiring HIT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
        hit.cancel(backend)
        hitcancelled = true
      }
    }
  }
  private def process_assignment[A <: Answer](q: MTurkQuestion, a: Assignment, hit_id: String, t: Thunk[A], use_disq: Boolean) {
    Utilities.DebugLog("Processing assignment...", LogLevel.WARN, LogType.ADAPTER, t.question.id)

    // mark as RETRIEVED
    t.state = SchedulerState.RETRIEVED

    // convert assignment XML to Answer
    t.answer = q.answer(a, t.is_dual).asInstanceOf[A]

    // assign worker_id to thunk now that we know it
    t.worker_id = Some(a.getWorkerId())

    // disqualify worker
    if (use_disq) {
      disqualify_worker(q, a.getWorkerId, t.question.id)
    }

    // pair assignment with thunk
    q.thunk_assnid_map += (t -> a.getAssignmentId)  // I believe that .getAssignmentId is just a local getter

    // write custominfo
    t.answer.custom_info = Some(new MTurkAnswerCustomInfo(a.getAssignmentId, hit_id).toString)
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

  private def hits_are_running(hits: List[AutomanHIT]) = {
    hits.filter{_.state == HITState.RUNNING}.size > 0
  }

  private def thunks_are_running[A <: Answer](ts: List[Thunk[A]]) {
    // mark timeouts
    ts.filter{_.state == SchedulerState.RUNNING}.foreach { t =>
      if(t.expires_at.before(new Date())) {
        Utilities.DebugLog("Thunk timeout.", LogLevel.WARN, LogType.ADAPTER, t.question.id)
        t.state = SchedulerState.TIMEOUT
      }
    }
  }

  def access_key_id: String = _access_key_id match { case Some(id) => id; case None => "" }
  def access_key_id_=(id: String) { _access_key_id = Some(id) }
  def get_budget_from_backend(): BigDecimal = {
    // budget requests are made as soon as is possible
    syncCommWait { () =>
      scheduled_get_budget()
    }
  }
  private def scheduled_get_budget(): BigDecimal = {
    Utilities.DebugLog("Getting budget.", LogLevel.INFO, LogType.ADAPTER, null)

    _service match {
      case Some(s) => {
        _budget = s.getAccountBalance
        _budget
      }
      case None => {
        throw MTurkAdapterNotInitialized("MTurkAdapter must be initialized before attempting to communicate.")
      }
    }
  }
  override def question_shutdown_hook(q: Question): Unit = {
    // cleanup qualifications
    cleanup_qualifications(q)
  }
  // call this in each question's shutdown hook
  private def cleanup_qualifications(q: Question) : Unit = {
    val req = q match {
      case mtq:MTurkQuestion => {
        DisposeQualsReq(mtq)
      }
      case _ => throw new Exception("Impossible error.")
    }

    // enqueue
    synchronized {
      _dispose_quals_queue.enqueue(req)
    }

    initWorkerIfNeeded()

    // wait for response
    // while loop is because the JVM is
    // permitted to send spurious wakeups
    while(synchronized { !_disposal_completions.contains(req) }) {
      req.synchronized {
        Utilities.DebugLog("Waiting for Adapter to do cleanup, putting Question Scheduler to sleep.", LogLevel.INFO, LogType.ADAPTER, null)
        req.wait() // block until answer is available
      }
    }
    // remove from set
    synchronized { _disposal_completions.remove(req) }
  }
  private def scheduled_cleanup_qualifications(q: MTurkQuestion) : Unit = {
    val s = _service match {
      case Some(s) => s
      case None => {
        throw MTurkAdapterNotInitialized("MTurkAdapter must be initialized before attempting to communicate.")
      }
    }

    q.qualifications.foreach { qual =>
      s.disposeQualificationType(qual.getQualificationTypeId)
    }
  }
  def poll_interval = _poll_interval_in_s
  def poll_interval_=(s: Int) { _poll_interval_in_s = s }
  def retriable_errors_=(re: Set[String]) { _retriable_errors = re }
  def retriable_errors = _retry_delay_millis
  def retry_attempts_=(ra: Int) { _retry_attempts = ra }
  def retry_attempts = _retry_attempts
  def retry_delay_millis_=(rdm: Int) { _retry_delay_millis = rdm }
  def retry_delay_millis = _retry_delay_millis
  def sandbox_mode = {
    _service_url == ClientConfig.SANDBOX_SERVICE_URL
  }
  def sandbox_mode_=(b: Boolean) {
    b match {
      case true => _service_url = ClientConfig.SANDBOX_SERVICE_URL
      case false => _service_url = ClientConfig.PRODUCTION_SERVICE_URL
    }
  }
  def secret_access_key: String = _secret_access_key match { case Some(s) => s; case None => "" }
  def secret_access_key_=(s: String) { _secret_access_key = Some(s) }
  def setup() {
    _service = Some(new RequesterService(this.toClientConfig))
  }
  def backend: RequesterService = _service match {
    case Some(s) => s
    case None => throw new MTurkAdapterNotInitialized("MTurkAdapter must be initialized before attempting to communicate.")
  }
  def toClientConfig : ClientConfig = synchronized {
    import scala.collection.JavaConversions

    val _config = new ClientConfig
    _config.setAccessKeyId(_access_key_id match { case Some(k) => k; case None => throw InvalidKeyIDException("access_key_id must be defined")})
    _config.setSecretAccessKey(_secret_access_key match { case Some(k) => k; case None => throw InvalidSecretKeyException("secret_access_key must be defined")})
    _config.setServiceURL(_service_url)
    _config.setRetriableErrors(JavaConversions.setAsJavaSet(_retriable_errors))
    _config.setRetryAttempts(_retry_attempts)
    _config.setRetryDelayMillis(_retry_delay_millis)
    _config
  }
}
