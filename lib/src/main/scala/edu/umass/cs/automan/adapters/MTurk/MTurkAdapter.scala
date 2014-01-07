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
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import edu.umass.cs.automan.adapters.MTurk.actor.messages._
import edu.umass.cs.automan.adapters.MTurk.actor.{MTurkAdapterProxyImpl, MTurkAdapterProxy, MTurkActor}

object MTurkAdapter {
  // initialization routine for DSL
  def apply(init: MTurkAdapter => Unit) : MTurkAdapter = {
    val a = new MTurkAdapter
    init(a)
    a.setup()                   // initializes MTurk SDK
    a.get_budget_from_backend() // asks MTurk for our current budget
    a
  }
  def question_for_thunks(ts: List[Thunk]) : Question = {
    // determine which question we've been asked about
    val tg = ts.groupBy(_.question)
    if(tg.size != 1) {
      throw new Exception("MTurkAdapter can only process groups of Thunks for the same Question.")
    }
    tg.head._1
  }
}

class MTurkAdapter extends AutomanAdapter[MTRadioButtonQuestion,
                                          MTCheckboxQuestion,
                                          MTFreeTextQuestion] {
  // actor setup
  private val _actor_system = ActorSystem("MTurkAdapter")
  implicit private val _comm_timeout = Timeout(10, java.util.concurrent.TimeUnit.SECONDS)

  // MTurk-specific fields
  private var _access_key_id: Option[String] = None
  private var _poll_interval_in_s : Int = 30
  private var _rate_limit_ms : Int = 1000 // TODO: getter and setter
  private var _retriable_errors = scala.collection.Set("Server.ServiceUnavailable")
  private var _retry_attempts : Int = 10
  private var _retry_delay_millis : Int = 1000
  private var _secret_access_key: Option[String] = None
  private var _service_url : String = ClientConfig.SANDBOX_SERVICE_URL
  private var _service : Option[RequesterService] = None
  private var _comm_actor : Option[MTurkAdapterProxy] = None

  // cleanup routine
  override def finalize {
    _actor_system.shutdown()
  }

  // DSL implementations
  def CheckboxQuestion(q: MTCheckboxQuestion => Unit) = MTCheckboxQuestion(q, this)
  def FreeTextQuestion(q: MTFreeTextQuestion => Unit) = MTFreeTextQuestion(q, this)
  def RadioButtonQuestion(q: MTRadioButtonQuestion => Unit) = MTRadioButtonQuestion(q, this)
  def Option(id: Symbol, text: String) = new MTQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new MTQuestionOption(id, text, image_url)

  // AutomanAdapter interface implementations -- TODO MUST BE RE-ENTRANT; USE ACTOR
  def accept(t: Thunk) : Unit = {
    _comm_actor match {
      case Some(actor) =>
        val future = actor.accept(AcceptRequest(t))
        Await.result(future, _comm_timeout.duration) // block until ack
      case None => throw new Exception("Adapter actor system not initialized.")
    }
  }
  def cancel(t: Thunk) {
    _comm_actor match {
      case Some(actor) =>
        val future = actor.cancel(CancelRequest(t))
        Await.result(future, _comm_timeout.duration) // block until ack
      case None => throw new Exception("Adapter actor system not initialized.")
    }
  }
  def post(ts: List[Thunk], dual: Boolean, exclude_worker_ids: List[String]) {

  }
  // put HIT's AssignmentId back into map or mark as PROCESSED
  def process_custom_info(t: Thunk, i: Option[String]) {
    val q = MTurkAdapter.question_for_thunks(List(t))
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
  def reject(t: Thunk) {
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
  def retrieve(ts: List[Thunk]) : List[Thunk] = {
    val question = mtquestion_for_thunks(ts)
    val auquestion = question_for_thunks(ts)
    val hits = question.hits.filter{_.state == HITState.RUNNING}

    // start by granting qualifications
    grant_qualification_requests(question, auquestion.blacklisted_workers, auquestion.id)

    // try grabbing something from each HIT
    hits.foreach { hit =>
    // get running thunks for each HIT
      val hts = mutable.Queue[Thunk]()
      hts ++= question.hit_thunk_map(hit).filter{_.state == SchedulerState.RUNNING}
      val assignments = hit.retrieve(backend)

      Utilities.DebugLog("There are " + assignments.size + " assignments available to process.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)

      // mark next available thunk as RETRIEVED for each answer
      assignments.foreach { assn => process_assignment(question, assn, hit.hit.getHITId, hts.dequeue()) }

      // timeout timed out Thunks and the HIT
      process_timeouts(hit, hts.toList)

      // check to see if we need to continue running this HIT
      mark_hit_complete(hit, hts.toList)
    }
    Utilities.DebugLog(ts.count(_.state == SchedulerState.RETRIEVED) + " thunks marked RETRIEVED.", LogLevel.INFO, LogType.ADAPTER, auquestion.id)
    ts
  }
  def schedule(q: MTRadioButtonQuestion): Future[RadioButtonAnswer] = future {
    val sched = new Scheduler(q, this, init_strategy(q), _memoizer, _thunklog)
    sched.run[RadioButtonAnswer]()
  }
  def schedule(q: MTCheckboxQuestion): Future[CheckboxAnswer] = future {
    val sched = new Scheduler(q, this, init_strategy(q), _memoizer, _thunklog)
    sched.run[CheckboxAnswer]()
  }
  def schedule(q: MTFreeTextQuestion): Future[FreeTextAnswer] = future {
    val sched = new Scheduler(q, this, init_strategy(q), _memoizer, _thunklog)
    sched.run[FreeTextAnswer]()
  }

  // getters and setters
  def access_key_id: String = _access_key_id match { case Some(id) => id; case None => "" }
  def access_key_id_=(id: String) { _access_key_id = Some(id) }
  def backend: RequesterService = _service match {
    case Some(s) => s
    case None => throw new MTurkAdapterNotInitialized("MTurkAdapter must be initialized before attempting to communicate.")
  }
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
    val my_service = new RequesterService(this.toClientConfig)
    _service = Some(my_service)
    _comm_actor = Some(
                    TypedActor(_actor_system).typedActorOf(
                      TypedProps(classOf[MTurkAdapterProxy],
                                 new MTurkAdapterProxyImpl(my_service, _rate_limit_ms)
                                )
                    )
                  )
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

  // Internal helper functions
  private def dequalify_worker(q: MTurkQuestion, worker_id: String, question_id: UUID, backend: RequesterService) {
    // grant dequalification Qualification
    // AMT checks whether worker's assigned value == 1; if so, not allowed
    if (q.worker_is_qualified(q.dequalification.getQualificationTypeId, worker_id)) {
      // the user may have asked for the dequalification for second-round thunks
      Utilities.DebugLog("Updating worker dequalification for " + worker_id + ".", LogLevel.INFO, LogType.ADAPTER, question_id)
      backend.updateQualificationScore(q.dequalification.getQualificationTypeId, worker_id, 1)
    } else {
      // otherwise, just grant it
      Utilities.DebugLog("Dequalifying worker " + worker_id + " from future work.", LogLevel.INFO, LogType.ADAPTER, question_id)
      backend.assignQualification(q.dequalification.getQualificationTypeId, worker_id, 1, false)
      q.qualify_worker(q.dequalification.getQualificationTypeId, worker_id)
    }
  }
  private def grant_qualification_requests(q: MTurkQuestion, blacklisted_workers: List[String], question_id: UUID) {
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
        if (qr.getQualificationTypeId == q.dequalification.getQualificationTypeId) {
          Utilities.DebugLog("Worker " + qr.getSubjectId + " requests one-time qualification; granting.", LogLevel.INFO, LogType.ADAPTER, question_id)
          backend.grantQualification(qr.getQualificationRequestId, 0)
          q.qualify_worker(qr.getQualificationTypeId, qr.getSubjectId)
        } else {
          throw new Exception("User-defined qualifications not yet supported.")
        }
      }
    }
  }
  private def init_strategy(q: Question): ValidationStrategy = {
    val s: ValidationStrategy = q.strategy_option match {
      case None => strategy.newInstance()
      case Some(sclass) => sclass.newInstance()
    }
    s.confidence = q.confidence
    s.num_possibilities = q.num_possibilities
    s
  }
  private def mark_hit_complete(hit: AutomanHIT, ts: List[Thunk]) {
    if (!ts.exists{_.state == SchedulerState.RUNNING}) {
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
  private def process_timeouts(hit: AutomanHIT, ts: List[Thunk]) {
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
  private def process_assignment(q: MTurkQuestion, assn: Assignment, hit_id: String, t: Thunk) {
    Utilities.DebugLog("Processing assignment...", LogLevel.WARN, LogType.ADAPTER, t.question.id)

    // mark as RETRIEVED
    t.state = SchedulerState.RETRIEVED

    // convert assignment XML to Answer
    t.answer = q.answer(assn, t.is_dual)

    // dequalify worker
    dequalify_worker(q, assn.getWorkerId, t.question.id, backend)

    // pair assignment with thunk
    q.thunk_assnid_map += (t -> assn.getAssignmentId)

    // write custominfo
    t.answer.custom_info = Some(new MTurkAnswerCustomInfo(assn.getAssignmentId, hit_id).toString)
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
