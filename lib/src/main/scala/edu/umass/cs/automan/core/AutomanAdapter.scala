package edu.umass.cs.automan.core

import answer._
import edu.umass.cs.automan.core.question._
import java.util.Locale
import java.text.NumberFormat
import actors.Future
import memoizer.{ThunkLogger, AutomanMemoizer}
import scheduler.Thunk
import strategy._

trait AutomanAdapter[RBQ <: RadioButtonQuestion,
                     CBQ <: CheckboxQuestion,
                     FTQ <: FreeTextQuestion] {
  // immutable; must be overridden
  protected val _memo_conn_string: String = "jdbc:derby:AutomanMemoDB;create=true"
  protected val _memo_user: String = ""
  protected val _memo_pass: String = ""
  protected val _thunk_conn_string: String = "jdbc:derby:ThunkLogDB;create=true"
  protected val _thunk_user: String = ""
  protected val _thunk_pass: String = ""

  // deferred because they depend on the above
  protected val _memoizer: AutomanMemoizer = new AutomanMemoizer(_memo_conn_string, _memo_user, _memo_pass)
  protected val _thunklog: ThunkLogger = new ThunkLogger(_thunk_conn_string, _thunk_user, _thunk_pass)

  // mutable
  protected var _budget: BigDecimal = 0.00
  protected var _confidence: Double = 0.95
  protected var _strategy: Class[_ <: ValidationStrategy] = classOf[DefaultStrategy]
  protected var _locale: Locale = Locale.getDefault

  // Backend control functions called by Scheduler -- MUST BE RE-ENTRANT!
  def accept(t: Thunk) : Unit
  def cancel(t: Thunk) : Unit
  def post(ts: List[Thunk], dual: Boolean, exclude_worker_ids: List[String]) : Unit
  def process_custom_info(t: Thunk, i: Option[String]) : Unit
  def reject(t: Thunk) : Unit
  def retrieve(ts: List[Thunk]) : Future[List[Thunk]]  // must return all thunks passed in
  def schedule(q: RBQ): Future[RadioButtonAnswer]
  def schedule(q: CBQ): Future[CheckboxAnswer]
  def schedule(q: FTQ): Future[FreeTextAnswer]

  // Getters and Setters -- SYNCHRONIZED ON `THIS' INSTANCE
  def budget: BigDecimal = synchronized { _budget }
  def budget_=(b: BigDecimal) = synchronized { _budget = b }
  def confidence: Double = synchronized { _confidence }
  def confidence_=(c: Double) = synchronized { _confidence = c }
  def locale: Locale = synchronized { _locale }
  def locale_=(l: Locale) { _locale = l }
  def strategy = synchronized { _strategy }
  def strategy_=(s: Class[ValidationStrategy]) = synchronized { _strategy = s }
  def budget_formatted = synchronized {
    val dbudget = _budget.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
    val nf = NumberFormat.getCurrencyInstance(_locale)
    nf.setMinimumFractionDigits(1)
    nf.setMaximumFractionDigits(2)
    nf.format(dbudget.doubleValue())
  }

  // DSL
  def CheckboxQuestion(fq: CBQ => Unit) : Future[CheckboxAnswer]
  def FreeTextQuestion(fq: FTQ => Unit) : Future[FreeTextAnswer]
  def RadioButtonQuestion(fq: RBQ => Unit) : Future[RadioButtonAnswer]
  def Option(id: Symbol, text: String) : QuestionOption

//  // Global backend config
//  def get_budget_from_backend(): BigDecimal
}