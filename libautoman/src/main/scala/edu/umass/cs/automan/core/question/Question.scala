package edu.umass.cs.automan.core.question

import java.io.File
import java.util.{Date, UUID}
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.{MockAnswer, MockResponse}
import edu.umass.cs.automan.core.policy.price.PricePolicy
import edu.umass.cs.automan.core.policy.timeout.TimeoutPolicy
import edu.umass.cs.automan.core.policy.aggregation.{UserDefinableSpawnPolicy, MinimumSpawnPolicy, AggregationPolicy}
import edu.umass.cs.automan.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Question {
  type A <: Any
  type AA <: AbstractAnswer[A]
  type O <: Outcome[A]
  type AP <: AggregationPolicy
  type PP <: PricePolicy
  type TP <: TimeoutPolicy

  class QuestionStillExecutingException extends Exception

  protected var _before_filter: A => A = (a: A) => a
  protected var _budget: Option[BigDecimal] = None
  protected var _id: UUID = UUID.randomUUID()
  protected var _image: Option[File] = None
  protected var _image_alt_text: Option[String] = None
  protected var _image_url: Option[String] = None
  protected var _initial_worker_timeout_in_s: Int = 30
  protected var _payOnFailure: Boolean = true
  protected var _question_timeout_multiplier: Double = 100
  protected var _text: Option[String] = None
  protected var _title: Option[String] = None
  protected var _time_value_per_hour: Option[BigDecimal] = None
  protected var _update_frequency_ms: Int = 30000
  protected var _max_replicas: Option[Int] = None
  protected var _mock_answers = List[MockAnswer[A]]()
  protected var _wage: BigDecimal = 7.25  // per hour
  protected var _blacklisted_workers = List[String]()
  protected var _dry_run: Boolean = false
  protected var _dont_reject: Boolean = false
  protected var _dont_randomize_options: Boolean = false

  protected[automan] var _price_policy: Option[Class[PP]] = None
  protected[automan] var _price_policy_instance: PP = _
  protected[automan] var _timeout_policy: Option[Class[TP]] = None
  protected[automan] var _timeout_policy_instance: TP = _
  protected[automan] var _validation_policy: Option[Class[AP]] = None
  protected[automan] var _validation_policy_instance: AP = _
  protected[automan] var _minimum_spawn_policy: MinimumSpawnPolicy = UserDefinableSpawnPolicy(0)

  def before_filter_=(f: A => A) { _before_filter = f }
  def before_filter: A => A = _before_filter
  def blacklist_worker(worker_id: String) { _blacklisted_workers = worker_id :: _blacklisted_workers }
  def blacklisted_workers = _blacklisted_workers
  def budget: BigDecimal = _budget match { case Some(b) => b; case None => 1.00 }
  def budget_=(b: BigDecimal) { _budget = Some(b) }
  def dont_reject_=(r: Boolean) { _dont_reject = r }
  def dont_reject: Boolean = _dont_reject
  def dry_run_=(dr: Boolean) { _dry_run = dr }
  def dry_run: Boolean = _dry_run
  def id: UUID = _id
  def id_=(id: UUID) { _id = id }
  def id_string: String = _id.toString
  def image_alt_text: String = _image_alt_text match { case Some(x) => x; case None => "" }
  def image_alt_text_=(s: String) { _image_alt_text = Some(s) }
  def image_url: String = _image_url match { case Some(x) => x; case None => "" }
  def image_url_=(s: String) { _image_url = Some(s) }
  def max_replicas: Option[Int] = _max_replicas
  def max_replicas_=(m: Int) { _max_replicas = Some(m) }
  def memo_hash: String
  def minimum_spawn_policy_=(p: MinimumSpawnPolicy) { _minimum_spawn_policy = p }
  def minimum_spawn_policy: MinimumSpawnPolicy = _minimum_spawn_policy
  def mock_answers_=(answers: List[MockAnswer[A]]) { _mock_answers = answers }
  def mock_answers: List[MockAnswer[A]] = _mock_answers
  def pay_all_on_failure_=(pay: Boolean) { _payOnFailure = pay }
  def pay_all_on_failure: Boolean = _payOnFailure
  def question_timeout_multiplier_=(t: Double) { _question_timeout_multiplier = t }
  def question_timeout_multiplier: Double = _question_timeout_multiplier
  def strategy = _validation_policy match { case Some(vs) => vs; case None => null }
  def strategy_=(s: Class[AP]) { _validation_policy = Some(s) }
  def text: String = _text match { case Some(t) => t; case None => "Question not specified." }
  def text_=(s: String) { _text = Some(s) }
  def time_value_per_hour: BigDecimal = _time_value_per_hour match { case Some(v) => v; case None => _wage }
  def time_value_per_hour_=(v: BigDecimal) { _time_value_per_hour = Some(v) }
  def title: String = _title match { case Some(t) => t; case None => text }
  def title_=(t: String) { _title = Some(t)}
  def update_frequency_ms : Int = _update_frequency_ms
  def update_frequency_ms_=(ms: Int) { _update_frequency_ms = ms }
  def wage: BigDecimal = _wage
  def wage_=(w: BigDecimal) { _wage = w }
  def initial_worker_timeout_in_s_=(t: Int) { _initial_worker_timeout_in_s = t }
  def initial_worker_timeout_in_s: Int = _initial_worker_timeout_in_s

  // private methods
  private[automan] def init_validation_policy(): Unit
  private[automan] def init_price_policy(): Unit
  private[automan] def init_timeout_policy(): Unit
  protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID) : MockResponse
  private[automan] def validation_policy_instance = _validation_policy_instance
  private[automan] def price_policy_instance = _price_policy_instance
  private[automan] def timeout_policy_instance = _timeout_policy_instance
  protected[automan] def schedulerFuture(adapter: AutomanAdapter) : Future[AA] = {
    Future{
      blocking {
        startScheduler(adapter)
      }
    }
  }
  protected[automan] def startScheduler(adapter: AutomanAdapter) : AA = {
    blocking {
      new Scheduler(this, adapter).run().asInstanceOf[AA]
    }
  }
  protected[automan] def getOutcome(adapter: AutomanAdapter) : O
  protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter) : O
  protected[automan] def getQuestionType: QuestionType
  protected[automan] def questionStartupHook() {}
  protected[automan] def questionShutdownHook() {}
}