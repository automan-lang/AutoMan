package edu.umass.cs.automan.core

import answer._
import edu.umass.cs.automan.core.question._
import java.util.Locale
import java.text.NumberFormat
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import memoizer.{ThunkLogger, AutomanMemoizer}
import scheduler.Thunk
import strategy._

abstract class AutomanAdapter {
  type RBQ <: RadioButtonQuestion                 // answer scalar
  type RBDQ <: RadioButtonDistributionQuestion    // answer vector
  type CBQ <: CheckboxQuestion                    // answer scalar
  type FTQ <: FreeTextQuestion                    // answer scalar
  protected var _budget: BigDecimal = 0.00
  protected var _confidence: Double = 0.95
  protected var _locale: Locale = Locale.getDefault
  protected var _memoizer: AutomanMemoizer = _
  protected var _memo_db: String = "AutomanMemoDB"
  protected def _memo_conn_string: String = "jdbc:derby:" + _memo_db + ";create=true"
  protected var _memo_user: String = ""
  protected var _memo_pass: String = ""
  protected var _thunklog: ThunkLogger = _
  protected var _thunk_db: String = "ThunkLogDB"
  protected var _thunk_conn_string: String = "jdbc:derby:" + _thunk_db + ";create=true"
  protected var _thunk_user: String = ""
  protected var _thunk_pass: String = ""

  // getters and setters
  def accept[A](t: Thunk[A])
  def budget: BigDecimal = _budget
  def budget_=(b: BigDecimal) { _budget = b }
  def cancel[A](t: Thunk[A])
  def confidence: Double = _confidence
  def confidence_=(c: Double) { _confidence = c }
  def memo_init() {
    _memoizer = new AutomanMemoizer(_memo_conn_string, _memo_user, _memo_pass)
  }
  def thunklog_init() {
    _thunklog = new ThunkLogger(_thunk_conn_string, _thunk_user, _thunk_pass)
  }
  def post[A](ts: List[Thunk[A]], dual: Boolean, exclude_worker_ids: List[String])
  def process_custom_info[A](t: Thunk[A], i: Option[String])
  def reject[A](t: Thunk[A])
  def retrieve[A](ts: List[Thunk[A]]) : List[Thunk[A]]  // returns all thunks passed in
  def question_startup_hook(q: Question): Unit = {}
  def question_shutdown_hook(q: Question): Unit = {}

  // Question creation
  def CheckboxQuestion(fq: CBQ => Unit) : Future[CheckboxAnswer]
  def FreeTextQuestion(fq: FTQ => Unit) : Future[FreeTextAnswer]
  def RadioButtonQuestion(fq: RBQ => Unit) : Future[RadioButtonAnswer]
  def RadioButtonDistributionQuestion(fq: RBDQ => Unit) : Future[Set[RadioButtonAnswer]]
  
  // Option creation
  def Option(id: Symbol, text: String) : QuestionOption

  // Global backend config
  def budget_formatted = {
    val dbudget = _budget.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
    val nf = NumberFormat.getCurrencyInstance(_locale)
    nf.setMinimumFractionDigits(1)
    nf.setMaximumFractionDigits(2)
    nf.format(dbudget.doubleValue())
  }
  def get_budget_from_backend(): BigDecimal
  def locale: Locale = _locale
  def locale_=(l: Locale) { _locale = l }
  def schedule(q: RBQ): Future[RadioButtonAnswer]
  def schedule(q: RBDQ): Future[Set[RadioButtonAnswer]]
  def schedule(q: CBQ): Future[CheckboxAnswer]
  def schedule(q: FTQ): Future[FreeTextAnswer]
}