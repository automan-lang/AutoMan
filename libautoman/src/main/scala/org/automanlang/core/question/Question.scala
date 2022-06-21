package org.automanlang.core.question

import java.io.File
import java.util.{Date, UUID}
import org.automanlang.core.{AutomanAdapter, MagicNumbers}
import org.automanlang.core.answer._
import org.automanlang.core.info.QuestionType.QuestionType
import org.automanlang.core.mock.{MockAnswer, MockResponse}
import org.automanlang.core.policy.price.PricePolicy
import org.automanlang.core.policy.timeout.TimeoutPolicy
import org.automanlang.core.policy.aggregation.{AggregationPolicy, MinimumSpawnPolicy, UserDefinableSpawnPolicy}
import org.automanlang.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

trait Question {
  type A <: Any			// return type of the function (what you get when you call .value)
  type AA <: AbstractAnswer[A]	// an instance of scheduler
  type O <: Outcome[A]		// outcome is value returned by the scheduler
  type AP <: AggregationPolicy	// how to derive a scalar value of type A from the distribution of values
  type PP <: PricePolicy	// how to determine reward
  type TP <: TimeoutPolicy	// how long to run the job

  class QuestionStillExecutingException extends Exception

  protected var _before_filter: A => A = (a: A) => a
  protected var _budget: Option[BigDecimal] = None
  protected var _banned_workers = List[String]()
  protected var _dry_run: Boolean = false
  protected var _dont_reject: Boolean = false
  protected var _dont_randomize_options: Boolean = false
  protected var _id: UUID = UUID.randomUUID()
  protected var _image: Option[File] = None
  protected var _image_alt_text: Option[String] = None
  protected var _image_url: Option[String] = None
  protected var _initial_worker_timeout_in_s: Int = 30
  protected var _max_replicas: Option[Int] = None
  protected var _mock_answers = Iterable[MockAnswer[A]]()
  protected var _name: String = "" // name of question (default title?)
  protected var _payOnFailure: Boolean = true
  protected var _question_timeout_multiplier: Double = MagicNumbers.QuestionTimeoutMultiplier
  protected var _text: Option[String] = None
  protected var _title: Option[String] = None
  protected var _time_value_per_hour: Option[BigDecimal] = None
  protected var _update_frequency_ms: Int = MagicNumbers.UpdateFrequencyMs
  protected var _wage: BigDecimal = MagicNumbers.USFederalMinimumWage


  protected[automanlang] var _price_policy: Option[Class[PP]] = None
  protected[automanlang] var _price_policy_instance: PP = _
  protected[automanlang] var _timeout_policy: Option[Class[TP]] = None
  protected[automanlang] var _timeout_policy_instance: TP = _
  protected[automanlang] var _validation_policy: Option[Class[AP]] = None
  protected[automanlang] var _validation_policy_instance: AP = _
  protected[automanlang] var _minimum_spawn_policy: MinimumSpawnPolicy = UserDefinableSpawnPolicy(0)

  def before_filter_=(f: A => A) { _before_filter = f }
  def before_filter: A => A = _before_filter
  def ban_worker(worker_id: String) { _banned_workers = worker_id :: _banned_workers }
  def banned_workers = _banned_workers
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
  def initial_worker_timeout_in_s_=(t: Int) { _initial_worker_timeout_in_s = t }
  def initial_worker_timeout_in_s: Int = _initial_worker_timeout_in_s
  def isSurvey: Boolean = false // false by default; override in Survey
  def max_replicas: Option[Int] = _max_replicas
  def max_replicas_=(m: Int) { _max_replicas = Some(m) }
  def memo_hash: String
  def minimum_spawn_policy_=(p: MinimumSpawnPolicy) { _minimum_spawn_policy = p }
  def minimum_spawn_policy: MinimumSpawnPolicy = _minimum_spawn_policy
  def mock_answers_=(answers: Iterable[MockAnswer[A]]) { _mock_answers = answers }
  def mock_answers: Iterable[MockAnswer[A]] = _mock_answers
  def name: String = _name
  def name_=(name: String) { _name = name }
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

  // Output path of CSV file
  protected var _csv_output: Option[String] = None
  def csv_output: String = _csv_output match { case Some(t) => t; case None => "Output not specified." }
  def csv_output_=(s: String): Unit = { _csv_output = Some(s) }

  // private methods
  private[automanlang] def init_validation_policy(): Unit
  private[automanlang] def init_price_policy(): Unit
  private[automanlang] def init_timeout_policy(): Unit
  protected[automanlang] def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID) : MockResponse
  private[automanlang] def validation_policy_instance = _validation_policy_instance
  protected[automanlang] def prettyPrintAnswer(answer: A) : String
  private[automanlang] def price_policy_instance = _price_policy_instance
  private[automanlang] def timeout_policy_instance = _timeout_policy_instance
  protected[automanlang] def schedulerFuture(adapter: AutomanAdapter) : Future[AA] = {
    Future{
      startScheduler(adapter)
    }
  }
  protected[automanlang] def startScheduler(adapter: AutomanAdapter) : AA = {
    blocking {
      new Scheduler(this, adapter).run().asInstanceOf[AA]
    }
  }
  protected[automanlang] def getOutcome(adapter: AutomanAdapter) : O
  protected[automanlang] def composeOutcome(o: O, adapter: AutomanAdapter) : O
  protected[automanlang] def getQuestionType: QuestionType
  protected[automanlang] def questionStartupHook() {}
  protected[automanlang] def questionShutdownHook() {}
}
