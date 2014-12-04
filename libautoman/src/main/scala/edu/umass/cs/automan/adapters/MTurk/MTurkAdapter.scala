package edu.umass.cs.automan.adapters.MTurk

import java.util.Date

import com.amazonaws.mturk.util.ClientConfig
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.core.question._
import question._
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.{Utilities, AutomanAdapter}
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.adapters.MTurk.connectionpool._

object MTurkAdapter {
  def apply(init: MTurkAdapter => Unit) : MTurkAdapter = {
    val mta = new MTurkAdapter
    init(mta)                     // assign values to fields in anonymous constructor
    mta.init()                    // run superclass initializer with values from previous line
    mta.setup()                   // initializes MTurk SDK
    mta.get_budget_from_backend() // asks MTurk for our current budget
    mta
  }
}

class MTurkAdapter extends AutomanAdapter {
  // these types provide MTurk implementations for
  // AutomanAdapter virtual methods
  override type RBQ = MTRadioButtonQuestion
  override type RBDQ = MTRadioButtonDistributionQuestion
  override type CBQ = MTCheckboxQuestion
  override type FTQ = MTFreeTextQuestion

  private val SLEEP_MS = 500
  private val SHUTDOWN_DELAY_MS = SLEEP_MS * 10

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

  protected def RBQFactory() = new MTRadioButtonQuestion
  protected def CBQFactory() = new MTCheckboxQuestion
  protected def FTQFactory() = new MTFreeTextQuestion
  protected def RBDQFactory() = new MTRadioButtonDistributionQuestion

  def Option(id: Symbol, text: String) = new MTQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new MTQuestionOption(id, text, image_url)

  protected[automan] def accept[A <: Answer](t: Thunk[A]) = run_if_initialized((p: Pool) => p.accept(t))
  protected[automan] def cancel[A <: Answer](t: Thunk[A]) = run_if_initialized((p: Pool) => p.cancel(t))
  protected[automan] def get_budget_from_backend() = run_if_initialized((p: Pool) => p.budget())
  protected[automan] def post[A <: Answer](ts: List[Thunk[A]], exclude_worker_ids: List[String]) = {
    run_if_initialized((p: Pool) => {
      p.post(ts, exclude_worker_ids)

      // mark thunks as RUNNING so that the scheduler
      // knows to attempt to retrieve their answers later
      ts.foreach { _.state = SchedulerState.RUNNING }
    })
  }
  protected[automan] def process_custom_info[A <: Answer](t: Thunk[A], i: Option[String]) =
    run_if_initialized((p: Pool) => p.process_custom_info(t, i))
  protected[automan] def reject[A <: Answer](t: Thunk[A]) = run_if_initialized((p: Pool) => p.reject(t))
  protected[automan] def retrieve[A <: Answer](ts: List[Thunk[A]]) =
    run_if_initialized((p: Pool) => p.retrieve(ts))
  protected[automan] override def question_shutdown_hook(q: Question): Unit = {
    // cleanup qualifications
    run_if_initialized((p: Pool) => p.cleanup_qualifications(q))
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
    _service = Some(rs)
    _pool = Some(new Pool(this, rs, SLEEP_MS, SHUTDOWN_DELAY_MS))
  }
  private def toClientConfig : ClientConfig = lock { () =>
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
