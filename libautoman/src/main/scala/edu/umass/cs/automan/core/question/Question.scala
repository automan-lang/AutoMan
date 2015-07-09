package edu.umass.cs.automan.core.question

import java.io.File
import java.util.UUID
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractAnswer, Outcome}
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.policy.price.PricePolicy
import edu.umass.cs.automan.core.policy.timeout.TimeoutPolicy
import edu.umass.cs.automan.core.policy.validation.ValidationPolicy

abstract class Question {
  type A <: Any
  type AA <: AbstractAnswer[A]
  type O <: Outcome[A]
  type VP <: ValidationPolicy
  type PP <: PricePolicy
  type TP <: TimeoutPolicy

  class QuestionStillExecutingException extends Exception

  protected var _budget: Option[BigDecimal] = None
  protected var _id: UUID = UUID.randomUUID()
  protected var _image: Option[File] = None
  protected var _image_alt_text: Option[String] = None
  protected var _image_url: Option[String] = None
  protected var _initial_worker_timeout_in_s: Int = 30
  protected var _question_timeout_multiplier: Double = 100
  protected var _text: Option[String] = None
  protected var _title: Option[String] = None
  protected var _time_value_per_hour: Option[BigDecimal] = None
  protected var _max_replicas: Option[Int] = None
  protected var _mock_answers = List[A]()
  protected var _wage: BigDecimal = 7.25  // per hour
  protected var _blacklisted_workers = List[String]()
  protected var _dry_run: Boolean = false
  protected var _dont_reject: Boolean = false
  protected var _dont_randomize_options: Boolean = false

  protected[automan] var _price_policy: Option[Class[PP]] = None
  protected[automan] var _price_policy_instance: PP = _
  protected[automan] var _timeout_policy: Option[Class[TP]] = None
  protected[automan] var _timeout_policy_instance: TP = _
  protected[automan] var _validation_policy: Option[Class[VP]] = None
  protected[automan] var _validation_policy_instance: VP = _

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
  def image: File = _image match { case Some(f) => f; case None => null }
  def image_=(f: File) { _image = Some(f) }
  def max_replicas: Option[Int] = _max_replicas
  def max_replicas_=(m: Int) { _max_replicas = Some(m) }
  def memo_hash: String
  def mock_answers_=(answers: List[A]) { _mock_answers = answers }
  def mock_answers: List[A] = _mock_answers
  def num_possibilities: BigInt
  def question_timeout_multiplier_=(t: Double) { _question_timeout_multiplier = t }
  def question_timeout_multiplier: Double = _question_timeout_multiplier
  def strategy = _validation_policy match { case Some(vs) => vs; case None => null }
  def strategy_=(s: Class[VP]) { _validation_policy = Some(s) }
  def text: String = _text match { case Some(t) => t; case None => "Question not specified." }
  def text_=(s: String) { _text = Some(s) }
  def time_value_per_hour: BigDecimal = _time_value_per_hour match { case Some(v) => v; case None => _wage }
  def time_value_per_hour_=(v: BigDecimal) { _time_value_per_hour = Some(v) }
  def title: String = _title match { case Some(t) => t; case None => text }
  def title_=(t: String) { _title = Some(t)}
  def wage: BigDecimal = _wage
  def wage_=(w: BigDecimal) { _wage = w }
  def initial_worker_timeout_in_s_=(t: Int) { _initial_worker_timeout_in_s = t }
  def initial_worker_timeout_in_s: Int = _initial_worker_timeout_in_s

  // private methods
  private[automan] def init_validation_policy(): Unit
  private[automan] def init_price_policy(): Unit
  private[automan] def init_timeout_policy(): Unit
  protected[automan] def toMockResponse(question_id: UUID, a: A) : MockResponse
  private[automan] def validation_policy_instance = _validation_policy_instance
  private[automan] def price_policy_instance = _price_policy_instance
  private[automan] def timeout_policy_instance = _timeout_policy_instance
  protected[automan] def getOutcome(adapter: AutomanAdapter,
                                    memo: Memo,
                                    poll_interval_in_s: Int) : O
  protected[automan] def getQuestionType: QuestionType
  protected[automan] def questionStartupHook() {}
  protected[automan] def questionShutdownHook() {}
}