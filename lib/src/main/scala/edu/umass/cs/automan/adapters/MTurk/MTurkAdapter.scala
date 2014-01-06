package edu.umass.cs.automan.adapters.MTurk

import com.amazonaws.mturk.util.ClientConfig
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.core.question._
import actors.Future
import actors.Futures._
import edu.umass.cs.automan.core.strategy.ValidationStrategy
import edu.umass.cs.automan.adapters.MTurk.memoizer.MTurkAnswerCustomInfo
import edu.umass.cs.automan.adapters.MTurk.question._
import edu.umass.cs.automan.core.scheduler.{Scheduler, SchedulerState, Thunk}
import edu.umass.cs.automan.core._
import java.util.{UUID, Date}
import com.amazonaws.mturk.requester._
import scala.Predef._
import edu.umass.cs.automan.core.answer.{FreeTextAnswer, Answer, RadioButtonAnswer, CheckboxAnswer}
import scala.collection.mutable
import scala.Some

class MTurkAdapterState extends AdapterState {
  private var _access_key_id: Option[String] = None
  private var _poll_interval_in_s : Int = 30
  private var _retriable_errors = scala.collection.Set("Server.ServiceUnavailable")
  private var _retry_attempts : Int = 10
  private var _retry_delay_millis : Int = 1000
  private var _secret_access_key: Option[String] = None
  private var _service_url : String = ClientConfig.SANDBOX_SERVICE_URL
  private var _service : Option[RequesterService] = None

  def access_key_id: String = _access_key_id match { case Some(id) => id; case None => "" }
  def access_key_id_=(id: String) { _access_key_id = Some(id) }
  def get_budget_from_backend(): BigDecimal = {
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
  def toClientConfig : ClientConfig = {
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

object MTurkAdapter extends AutomanAdapter[MTurkAdapterState,
                                           MTRadioButtonQuestion,
                                           MTCheckboxQuestion,
                                           MTFreeTextQuestion] {
  // AutomanAdapter interface implementations
  def accept(t: Thunk, a: MTurkAdapterState) {
    t.question match {
      case mtq:MTurkQuestion => {
        a.backend.approveAssignment(mtq.thunk_assnid_map(t), "Thanks!")
        t.state = SchedulerState.ACCEPTED
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
  def cancel(t: Thunk, a: MTurkAdapterState) {
    t.question match {
      case mtq:MTurkQuestion => {
        mtq.hits.filter{_.state == HITState.RUNNING}.foreach { hit =>
          a.backend.forceExpireHIT(hit.hit.getHITId)
          hit.state = HITState.RESOLVED
        }
        t.state = SchedulerState.REJECTED
      }
      case _ => throw new Exception("Impossible error.")
    }
  }
  def post(ts: List[Thunk], dual: Boolean, exclude_worker_ids: List[String], a: MTurkAdapterState) {
    val question = question_for_thunks(ts)
    val mtquestion = question match { case mtq: MTurkQuestion => mtq; case _ => throw new Exception("Impossible.") }
    val qualify_early = if (question.blacklisted_workers.size > 0) true else false
    val quals = get_qualifications(mtquestion, ts.head.question.text, qualify_early, question.id)

    // Build HIT and add it to post queue
    mtquestion match {
      case rbq: MTRadioButtonQuestion => {
        if (!dual) {
          // TODO: _post_queue += new Tuple2(rbq, rbq.build_hit(ts, is_dual = false, quals))
        }
      }
      case cbq: MTCheckboxQuestion => {
        // TODO: _post_queue += new Tuple2(cbq, cbq.build_hit(ts, dual, quals))
      }
      case ftq: MTFreeTextQuestion => {
        // TODO: _post_queue += new Tuple2(ftq, ftq.build_hit(ts, is_dual = false, quals))
      }
      case _ => throw new Exception("Question type not yet supported.  Question class is " + mtquestion.getClass)
    }
    ts.foreach { _.state = SchedulerState.RUNNING }
    // TODO: _retrieval_queue ++= ts
  }
  def reject(t: Thunk, a: MTurkAdapterState) {
    t.question match {
      case mtq:MTurkQuestion => {
        a.backend.rejectAssignment(mtq.thunk_assnid_map(t), "Your answer is incorrect with a probability >" + a.confidence + ".")
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
  def retrieve(ts: List[Thunk], a: MTurkAdapterState) : List[Thunk] = {
    val question = mtquestion_for_thunks(ts)
    val auquestion = question_for_thunks(ts)
    val hits = question.hits.filter{_.state == HITState.RUNNING}

    // start by granting qualifications
    grant_qualification_requests(question, auquestion.blacklisted_workers, auquestion.id, a)

    // try grabbing something from each HIT
    hits.foreach { hit =>
    // get running thunks for each HIT
      val hts = mutable.Queue[Thunk]()
      hts ++= question.hit_thunk_map(hit).filter{_.state == SchedulerState.RUNNING}
      val assignments = hit.retrieve(a.backend)

      Utilities.DebugLog("There are " + assignments.size + " assignments available to process.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)

      // mark next available thunk as RETRIEVED for each answer
      assignments.foreach { assn => process_assignment(question, assn, hit.hit.getHITId, hts.dequeue(), a) }

      // timeout timed out Thunks and the HIT
      process_timeouts(hit, hts.toList, a)

      // check to see if we need to continue running this HIT
      mark_hit_complete(hit, hts.toList)
    }

    Utilities.DebugLog(ts.filter{_.state == SchedulerState.RETRIEVED}.size + " thunks marked RETRIEVED.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)
    ts
  }
  def schedule(q: MTRadioButtonQuestion, a: MTurkAdapterState): Future[RadioButtonAnswer] = future {
    val sched = new Scheduler(q, a, init_strategy(q), _memoizer, _thunklog)
    sched.run[RadioButtonAnswer]()
  }
  def schedule(q: MTCheckboxQuestion, a: MTurkAdapterState): Future[CheckboxAnswer] = future {
    val sched = new Scheduler(q, this, init_strategy(q), _memoizer, _thunklog)
    sched.run[CheckboxAnswer]()
  }
  def schedule(q: MTFreeTextQuestion, a: MTurkAdapterState): Future[FreeTextAnswer] = future {
    val sched = new Scheduler(q, this, init_strategy(q), _memoizer, _thunklog)
    sched.run[FreeTextAnswer]()
  }

  // DSL implementations
  def CheckboxQuestion(q: MTCheckboxQuestion => Unit) = MTCheckboxQuestion(q, this)
  def FreeTextQuestion(q: MTFreeTextQuestion => Unit) = MTFreeTextQuestion(q, this)
  def RadioButtonQuestion(q: MTRadioButtonQuestion => Unit) = MTRadioButtonQuestion(q, this)
  def Option(id: Symbol, text: String) = new MTQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new MTQuestionOption(id, text, image_url)

  // initialization routine
  def apply(init: MTurkAdapterState => Unit) : MTurkAdapterState = {
    val p = new MTurkAdapterState
    init(p)
    p.setup()                   // initializes MTurk SDK
    p.get_budget_from_backend() // asks MTurk for our current budget
    p
  }

  // Internal helper functions
  private def dequalify_worker(q: MTurkQuestion, worker_id: String, question_id: UUID, a: MTurkAdapterState) {
    // grant dequalification Qualification
    // AMT checks whether worker's assigned value == 1; if so, not allowed
    if (q.worker_is_qualified(q.dequalification.getQualificationTypeId, worker_id)) {
      // the user may have asked for the dequalification for second-round thunks
      Utilities.DebugLog("Updating worker dequalification for " + worker_id + ".", LogLevel.INFO, LogType.ADAPTER, question_id)
      a.backend.updateQualificationScore(q.dequalification.getQualificationTypeId, worker_id, 1)
    } else {
      // otherwise, just grant it
      Utilities.DebugLog("Dequalifying worker " + worker_id + " from future work.", LogLevel.INFO, LogType.ADAPTER, question_id)
      a.backend.assignQualification(q.dequalification.getQualificationTypeId, worker_id, 1, false)
      q.qualify_worker(q.dequalification.getQualificationTypeId, worker_id)
    }
  }
  private def get_qualifications(q: MTurkQuestion, title: String, qualify_early: Boolean, question_id: UUID, a: MTurkAdapterState) : List[QualificationRequirement] = {
    // The first qualification always needs to be the special
    // "dequalification" type so that we may grant it as soon as
    // a worker completes some work.
    if (q.firstrun) {
      Utilities.DebugLog("This is the task's first run; creating dequalification.",LogLevel.INFO,LogType.ADAPTER,question_id)
      val qual : QualificationType = a.backend.createQualificationType("AutoMan " + UUID.randomUUID(),
            "automan", "AutoMan automatically generated Qualification (title: " + title + ")")
      val deq = new QualificationRequirement(qual.getQualificationTypeId, Comparator.NotEqualTo, 1, null, false)
      q.dequalification = deq
      q.firstrun = false
      // we need early qualifications; add anyway
      if (qualify_early) {
        q.qualifications = deq :: q.qualifications
      }
    } else if (!q.qualifications.contains(q.dequalification)) {
      // add the dequalification to the list of quals if this
      // isn't a first run and it isn't already there
      q.qualifications = q.dequalification :: q.qualifications
    }
    q.qualifications
  }
  private def grant_qualification_requests(q: MTurkQuestion, blacklisted_workers: List[String], question_id: UUID, a: MTurkAdapterState) {
    // get all requests for all qualifications on this HIT
    val qrs = q.qualifications.map { qual =>
      a.backend.getAllQualificationRequests(qual.getQualificationTypeId)
    }.flatten

    qrs.foreach { qr =>
      if (blacklisted_workers.contains(qr.getSubjectId)) {
        // we don't want blacklisted workers working on this
        Utilities.DebugLog("Worker " + qr.getSubjectId + " cannot request a qualification for a question that they are blacklisted for; rejecting request.", LogLevel.INFO, LogType.ADAPTER, question_id)
        a.backend.rejectQualificationRequest(qr.getQualificationRequestId, "You may not work on this particular hit for statistical purposes.")
      } else if (q.worker_is_qualified(qr.getQualificationTypeId, qr.getSubjectId)) {
        // we don't want to grant more than once
        Utilities.DebugLog("Worker " + qr.getSubjectId + " cannot request a qualification more than once; rejecting request.", LogLevel.INFO, LogType.ADAPTER, question_id)
        a.backend.rejectQualificationRequest(qr.getQualificationRequestId, "You cannot request this Qualification more than once.")
      } else {
        // grant
        if (qr.getQualificationTypeId == q.dequalification.getQualificationTypeId) {
          Utilities.DebugLog("Worker " + qr.getSubjectId + " requests one-time qualification; granting.", LogLevel.INFO, LogType.ADAPTER, question_id)
          a.backend.grantQualification(qr.getQualificationRequestId, 0)
          q.qualify_worker(qr.getQualificationTypeId, qr.getSubjectId)
        } else {
          throw new Exception("User-defined qualifications not yet supported.")
        }
      }
    }
  }
  private def init_strategy(q: Question, a: MTurkAdapterState): ValidationStrategy = {
    val s: ValidationStrategy = q.strategy_option match {
      case None => a.strategy.newInstance()
      case Some(sclass) => sclass.newInstance()
    }
    s.confidence = q.confidence
    s.num_possibilities = q.num_possibilities
    s
  }
  private def mark_hit_complete(hit: AutomanHIT, ts: List[Thunk]) {
    if (ts.filter{_.state == SchedulerState.RUNNING}.size == 0) {
      // we're done
      Utilities.DebugLog("HIT is RESOLVED.", LogLevel.INFO, LogType.ADAPTER, hit.id)
      hit.state = HITState.RESOLVED
    }
  }
  private def mtquestion_for_thunks(ts: List[Thunk]) : MTurkQuestion = {
    // determine which MT question we've been asked about
    question_for_thunks(ts) match {
      case mtq: MTurkQuestion => mtq
      case _ => throw new Exception("MTurkAdapter can only operate on Thunks for MTurkQuestions.")
    }
  }
  private def process_posts() {
    // TODO: process code
//    while(_post_queue.nonEmpty) {
//      // get next item
//      val (question, hit) = _post_queue.dequeue()
//      // after posting, set the hit_type_id field of the question
//      question.hit_type_id = hit.post(backend)
//      // sleep
//      Thread.sleep(_retry_delay_millis)
//    }
  }
  // put HIT's AssignmentId back into map or mark as PROCESSED
  private def process_custom_info(t: Thunk, i: Option[String]) {
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
  private def process_timeouts(hit: AutomanHIT, ts: List[Thunk], a: MTurkAdapterState) {
    var hitcancelled = false
    ts.filter{_.is_timedout}.foreach { t =>
      Utilities.DebugLog("HIT TIMED OUT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
      t.state = SchedulerState.TIMEOUT
      if (!hitcancelled) {
        Utilities.DebugLog("Force-expiring HIT.", LogLevel.WARN, LogType.ADAPTER, hit.id)
        hit.cancel(a.backend)
        hitcancelled = true
      }
    }
  }
  private def process_assignment(q: MTurkQuestion, assn: Assignment, hit_id: String, t: Thunk, a: MTurkAdapterState) {
    Utilities.DebugLog("Processing assignment...", LogLevel.WARN, LogType.ADAPTER, t.question.id)

    // mark as RETRIEVED
    t.state = SchedulerState.RETRIEVED

    // convert assignment XML to Answer
    t.answer = q.answer(assn, t.is_dual)

    // dequalify worker
    dequalify_worker(q, assn.getWorkerId, t.question.id, a)

    // pair assignment with thunk
    q.thunk_assnid_map += (t -> assn.getAssignmentId)

    // write custominfo
    t.answer.custom_info = Some(new MTurkAnswerCustomInfo(assn.getAssignmentId, hit_id).toString)
  }
  private def question_for_thunks(ts: List[Thunk]) : Question = {
    // determine which question we've been asked about
    val tg = ts.groupBy(_.question)
    if(tg.size != 1) {
      throw new Exception("MTurkAdapter can only process groups of Thunks for the same Question.")
    }
    tg.head._1
  }
//  private def hits_are_running(hits: List[AutomanHIT]) = {
//    hits.filter{_.state == HITState.RUNNING}.size > 0
//  }
//  private def thunks_are_running(ts: List[Thunk]) {
//    // mark timeouts
//    ts.filter{_.state == SchedulerState.RUNNING}.foreach { t =>
//      if(t.expires_at.before(new Date())) {
//        Utilities.DebugLog("Thunk timeout.", LogLevel.WARN, LogType.ADAPTER, t.question.id)
//        t.state = SchedulerState.TIMEOUT
//      }
//    }
//  }
}
