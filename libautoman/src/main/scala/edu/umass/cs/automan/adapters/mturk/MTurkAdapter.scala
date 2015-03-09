package edu.umass.cs.automan.adapters.mturk

import java.util.{Date, UUID, Locale}

import com.amazonaws.mturk.util.ClientConfig
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.connectionpool.Pool
import edu.umass.cs.automan.adapters.mturk.logging.MTMemo
import edu.umass.cs.automan.adapters.mturk.question.{MTurkQuestion, MTQuestionOption, MTRadioButtonQuestion}
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.AutomanAdapter

object MTurkAdapter {
  def apply(init: MTurkAdapter => Unit) : MTurkAdapter = {
    val mta = new MTurkAdapter
    init(mta)                     // assign values to fields in anonymous constructor
    mta.init()                    // run superclass initializer with values from previous line
    mta.setup()                   // initializes MTurk SDK
    mta.backend_budget() // asks MTurk for our current budget
    mta
  }
}

class MTurkAdapter extends AutomanAdapter {
  // these types provide MTurk implementations for
  // AutomanAdapter virtual methods
//  override type CBQ = MTCheckboxQuestion
//  override type CBDQ = MTCheckboxDistributionQuestion
//  override type FTQ = MTFreeTextQuestion
//  override type FTDQ = MTFreeTextDistributionQuestion
  override type RBQ = MTRadioButtonQuestion
//  override type RBDQ = MTRadioButtonDistributionQuestion
  override type MemoDB = MTMemo

  private val SLEEP_MS = 500

  private var _access_key_id: Option[String] = None
  private var _pool : Option[Pool] = None
  private var _retriable_errors = Set("Server.ServiceUnavailable")
  private var _retry_attempts : Int = 10
  private var _retry_delay_millis : Int = 1000
  private var _secret_access_key: Option[String] = None
  private var _service_url : String = ClientConfig.SANDBOX_SERVICE_URL
  private var _service : Option[RequesterService] = None

  // user-visible getters and setters
  def access_key_id: String = _access_key_id match { case Some(id) => id; case None => "" }
  def access_key_id_=(id: String) { _access_key_id = Some(id) }
  def locale: Locale = _locale
  def locale_=(l: Locale) { _locale = l }
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

//  protected def CBQFactory() = new MTCheckboxQuestion
//  protected def CBDQFactory() = new MTCheckboxDistributionQuestion
//  protected def FTQFactory() = new MTFreeTextQuestion
//  protected def FTDQFactory() = new MTFreeTextDistributionQuestion
  protected def RBQFactory() = new MTRadioButtonQuestion
//  protected def RBDQFactory() = new MTRadioButtonDistributionQuestion

  def Option(id: Symbol, text: String) = new MTQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new MTQuestionOption(id, text, image_url)

  protected[automan] def accept[A](t: Thunk[A]) = {
    assert(t.state == SchedulerState.ANSWERED)
    run_if_initialized((p: Pool) => p.accept(t))
  }
  protected[automan] def cancel[A](t: Thunk[A]) = run_if_initialized((p: Pool) => p.cancel(t))
  protected[automan] def backend_budget() = run_if_initialized((p: Pool) => p.backend_budget)
  protected[automan] def post[A](ts: List[Thunk[A]], exclude_worker_ids: List[String]) = {
    assert(ts.forall(_.state == SchedulerState.READY))

    run_if_initialized((p: Pool) => {
      // mark thunks as RUNNING so that the scheduler
      // knows to attempt to retrieve their answers later
      val ts2 = ts.map { _.copy_as_running() }
      p.post(ts2, exclude_worker_ids)
      ts2
    })
  }
  protected[automan] def reject[A](t: Thunk[A], correct_answer: String) = {
    assert(t.state == SchedulerState.ANSWERED)
    run_if_initialized((p: Pool) => p.reject(t, correct_answer))
  }
  protected[automan] def retrieve[A](ts: List[Thunk[A]]) = {
    assert(ts.forall(_.state == SchedulerState.RUNNING))
    run_if_initialized((p: Pool) => p.retrieve(ts))
  }
  override protected[automan] def question_shutdown_hook[A](q: Question[A]): Unit = {
    super.question_shutdown_hook(q)
    // cleanup qualifications
    run_if_initialized((p: Pool) => p.cleanup_qualifications(q.asInstanceOf[MTurkQuestion]))
  }

  // exception helper function
  private def run_if_initialized[U](f: Pool => U) : U = {
    _pool match {
      case Some(p) => f(p)
      case None => {
        throw MTurkAdapterNotInitialized("MTurkAdapter must be initialized before attempting to communicate.")
      }
    }
  }

  // initialization routines
  private def setup() {
    val rs = new RequesterService(this.toClientConfig)
    val pool = new Pool(rs, SLEEP_MS)
    _service = Some(rs)
    _memoizer.restore_mt_state(pool, rs)
    _pool = Some(pool)
  }
  private def toClientConfig : ClientConfig = {
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

  override protected[automan] def close(): Unit = {
    super.close()
    _pool match {
      case Some(p) => p.shutdown()
      case None => ()
    }
  }

  override protected[automan] def memo_init(): Unit = {
    _memoizer = new MTMemo(_log_config)
    _memoizer.init()
  }
  override protected def MemoDBFactory() : MemoDB = {
    new MTMemo(_log_config)
  }
}
