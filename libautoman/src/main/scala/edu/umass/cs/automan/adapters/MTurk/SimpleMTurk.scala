package edu.umass.cs.automan.adapters.MTurk

import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.ClientConfig

// this is a simple backend adapter for testing purposes only
// TODO: make base trait for MTurkAdapter
class SimpleMTurk(access_key: String, secret_key: String, sandbox_mode: Boolean) {
  private val _retriable_errors = Set("Server.ServiceUnavailable")
  private val _retry_attempts : Int = 10
  private val _retry_delay_millis : Int = 1000
  private var _service : Option[RequesterService] = None

  def service_url = if (sandbox_mode) { ClientConfig.SANDBOX_SERVICE_URL } else {  ClientConfig.PRODUCTION_SERVICE_URL }
  def setup() : SimpleMTurk = {
    _service = Some(new RequesterService(this.toClientConfig))
    this
  }
  def backend: RequesterService = _service match {
    case Some(s) => s
    case None => throw new MTurkAdapterNotInitialized("MTurkAdapter must be initialized before attempting to communicate.")
  }
  def toClientConfig : ClientConfig = synchronized {
    import scala.collection.JavaConversions

    val _config = new ClientConfig
    _config.setAccessKeyId(access_key)
    _config.setSecretAccessKey(secret_key)
    _config.setServiceURL(service_url)
    _config.setRetriableErrors(JavaConversions.setAsJavaSet(_retriable_errors))
    _config.setRetryAttempts(_retry_attempts)
    _config.setRetryDelayMillis(_retry_delay_millis)
    _config
  }
}
