package edu.umass.cs.automan.core.question

import java.util.UUID
import edu.umass.cs.automan.core.strategy.ValidationStrategy

abstract class Question {
  protected var _budget: Option[BigDecimal] = None
  protected var _confidence: Option[Double] = None
  protected var _id: UUID = UUID.randomUUID()
  protected var _image_alt_text: Option[String] = None
  protected var _image_url: Option[String] = None
  protected var _worker_timeout_in_s: Int = 30
  protected var _question_timeout_multiplier: Double = 100
  protected var _strategy: Option[Class[ValidationStrategy]] = None
  protected var _text: Option[String] = None
  protected var _title: Option[String] = None
  protected var _time_value_per_hour: Option[BigDecimal] = None
  protected var _max_replicas: Option[Int] = None
  protected var _wage: BigDecimal = 7.25  // per hour
  protected var _blacklisted_workers = List[String]()
  protected var _dry_run: Boolean = false
  protected var _dont_reject: Boolean = false

  def blacklist_worker(worker_id: String) { _blacklisted_workers = worker_id :: _blacklisted_workers }
  def blacklisted_workers = _blacklisted_workers
  def budget: BigDecimal = _budget match { case Some(b) => b; case None => 1.00 }
  def budget_=(b: BigDecimal) { _budget = Some(b) }
  def confidence: Double = _confidence match { case Some(c) => c; case None => 0.95 }
  def confidence_=(c: Double) { _confidence = Some(c) }
  def dont_reject_=(r: Boolean) { _dont_reject = r }
  def dont_reject: Boolean = _dont_reject
  def dry_run_=(dr: Boolean) { _dry_run = dr }
  def dry_run: Boolean = _dry_run
  def id: UUID = _id
  def id_string: String = _id.toString
  def image_alt_text: String = _image_alt_text match { case Some(x) => x; case None => "" }
  def image_alt_text_=(s: String) { _image_alt_text = Some(s) }
  def image_url: String = _image_url match { case Some(x) => x; case None => "" }
  def image_url_=(s: String) { _image_url = Some(s) }
  def memo_hash(dual: Boolean): String
  def num_possibilities: BigInt
  def question_timeout_in_s: Int = (_worker_timeout_in_s * _question_timeout_multiplier).toInt
  def question_timeout_multiplier_=(t: Double) { _question_timeout_multiplier = t }
  def question_timeout_multiplier: Double = _question_timeout_multiplier
  def reward : BigDecimal = {
    (_wage * _worker_timeout_in_s * (1.0/3600)).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
  }
  def strategy = _strategy match { case Some(vs) => vs; case None => null }
  def strategy_=(s: Class[ValidationStrategy]) { _strategy = Some(s) }
  def strategy_option = _strategy
  def text: String = _text match { case Some(t) => t; case None => "Question not specified." }
  def text_=(s: String) { _text = Some(s) }
  def time_value_per_hour: BigDecimal = _time_value_per_hour match { case Some(v) => v; case None => _wage }
  def time_value_per_hour_=(v: BigDecimal) { _time_value_per_hour = Some(v) }
  def title: String = _title match { case Some(t) => t; case None => title }
  def title_=(t: String) { _title = Some(t)}
  def max_replicas: Option[Int] = _max_replicas
  def max_replicas_=(m: Int) { _max_replicas = Some(m) }
  def wage: BigDecimal = _wage
  def wage_=(w: BigDecimal) { _wage = wage }
  def worker_timeout_in_s_=(t: Int) { _worker_timeout_in_s = t }
  def worker_timeout_in_s: Int = _worker_timeout_in_s
}