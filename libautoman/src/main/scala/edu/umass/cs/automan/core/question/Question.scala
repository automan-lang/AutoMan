package edu.umass.cs.automan.core.question

import java.io.File
import java.util.{Date, UUID}
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk, Scheduler}
import edu.umass.cs.automan.core.strategy.ValidationStrategy

abstract class Question[A] {
  type VS <: ValidationStrategy[A]

  class QuestionStillExecutingException extends Exception

  protected var _budget: Option[BigDecimal] = None
  protected var _id: UUID = UUID.randomUUID()
  protected var _image: Option[File] = None
  protected var _image_alt_text: Option[String] = None
  protected var _image_url: Option[String] = None
  protected var _worker_timeout_in_s: Int = 30
  protected var _question_timeout_multiplier: Double = 100
  protected var _strategy: Option[Class[VS]] = None
  protected var _strategy_instance: VS = _
  protected var _text: Option[String] = None
  protected var _title: Option[String] = None
  protected var _time_value_per_hour: Option[BigDecimal] = None
  protected var _max_replicas: Option[Int] = None
  protected var _wage: BigDecimal = 7.25  // per hour
  protected var _blacklisted_workers = List[String]()
  protected var _dry_run: Boolean = false
  protected var _dont_reject: Boolean = false
  protected var _dont_randomize_options: Boolean = false

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
  def num_possibilities: BigInt
  def question_timeout_in_s: Int = (_worker_timeout_in_s * _question_timeout_multiplier).toInt
  def question_timeout_multiplier_=(t: Double) { _question_timeout_multiplier = t }
  def question_timeout_multiplier: Double = _question_timeout_multiplier
  def reward : BigDecimal = { // this is what workers actually get paid per-task
    (_wage * _worker_timeout_in_s * (1.0/3600)).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
  }
  def strategy = _strategy match { case Some(vs) => vs; case None => null }
  def strategy_=(s: Class[VS]) { _strategy = Some(s) }
  def strategy_option = _strategy
  def text: String = _text match { case Some(t) => t; case None => "Question not specified." }
  def text_=(s: String) { _text = Some(s) }
  def time_value_per_hour: BigDecimal = _time_value_per_hour match { case Some(v) => v; case None => _wage }
  def time_value_per_hour_=(v: BigDecimal) { _time_value_per_hour = Some(v) }
  def title: String = _title match { case Some(t) => t; case None => text }
  def title_=(t: String) { _title = Some(t)}
  def wage: BigDecimal = _wage
  def wage_=(w: BigDecimal) { _wage = w }
  def worker_timeout_in_s_=(t: Int) { _worker_timeout_in_s = t }
  def worker_timeout_in_s: Int = _worker_timeout_in_s

  // private methods
  private[automan] def init_strategy(): Unit
  private[automan] def strategy_instance = _strategy_instance
  protected[automan] def getAnswer(scheduler: Scheduler[A]): Answer[A]
  protected[automan] def getQuestionType: QuestionType
}