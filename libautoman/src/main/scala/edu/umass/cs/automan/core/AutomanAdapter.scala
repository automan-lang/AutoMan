package edu.umass.cs.automan.core

import java.text.NumberFormat
import java.util.{UUID, Locale}
import edu.umass.cs.automan.core.info.{StateInfo, QuestionInfo}
import edu.umass.cs.automan.core.question._
import answer._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import memoizer.{ThunkLogger, AutomanMemoizer}
import edu.umass.cs.automan.core.scheduler.{Scheduler, Thunk}

abstract class AutomanAdapter {
  // question types are determined by adapter implementations
  // answer types are invariant
  type CBQ <: CheckboxQuestion                    // answer scalar
  type CBDQ <: CheckboxDistributionQuestion       // answer vector
  type FTQ <: FreeTextQuestion                    // answer scalar
  type FTDQ <: FreeTextDistributionQuestion       // answer vector
  type RBQ <: RadioButtonQuestion                 // answer scalar
  type RBDQ <: RadioButtonDistributionQuestion    // answer vector

  protected var _default_budget: BigDecimal = 5.00
  protected var _default_confidence: Double = 0.95
  protected var _locale: Locale = Locale.getDefault
  protected var _memoizer: Option[AutomanMemoizer] = None
  protected var _memo_db: String = "AutomanMemoDB"
  protected def _memo_conn_string: String = "jdbc:derby:" + _memo_db + ";create=true"
  protected var _memo_user: String = ""
  protected var _memo_pass: String = ""
  protected var _plugins: List[Class[_ <: Plugin]] = List.empty
  protected var _plugins_initialized: List[_ <: Plugin] = List.empty
  protected var _poll_interval_in_s : Int = 30
  protected var _schedulers: List[Scheduler] = List.empty
  protected var _thunklog: Option[ThunkLogger] = None
  protected var _thunk_db: String = "ThunkLogDB"
  protected var _thunk_conn_string: String = "jdbc:derby:" + _thunk_db + ";create=true"
  protected var _thunk_user: String = ""
  protected var _thunk_pass: String = ""
  protected var _use_memoization: Boolean = true

  // user-visible getters and setters
  def budget: BigDecimal = _default_budget
  def budget_=(b: BigDecimal) { _default_budget = b }
  def default_confidence: Double = _default_confidence
  def default_confidence_=(c: Double) { _default_confidence = c }
  def locale: Locale = _locale
  def locale_=(l: Locale) { _locale = l }
  def plugins: List[Class[_ <: Plugin]] = _plugins
  def plugins_=(ps: List[Class[_ <: Plugin]]) { _plugins = ps }
  def use_memoization = _use_memoization
  def use_memoization_=(um: Boolean) { _use_memoization = um }

  // marshaling calls
  protected[automan] def accept[A <: Answer](t: Thunk[A]) : Thunk[A]
  protected[automan] def cancel[A <: Answer](t: Thunk[A]) : Thunk[A]
  protected[automan] def post[A <: Answer](ts: List[Thunk[A]], exclude_worker_ids: List[String]) : List[Thunk[A]]
  protected[automan] def process_custom_info[A <: Answer](t: Thunk[A], i: Option[String]) : Thunk[A]
  protected[automan] def reject[A <: Answer](t: Thunk[A]) : Thunk[A]
  protected[automan] def retrieve[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]]  // returns all thunks passed in
  protected[automan] def question_startup_hook(q: Question): Unit = {}
  protected[automan] def question_shutdown_hook(q: Question): Unit = {}

  // end-user syntax: Question creation
  def CheckboxQuestion(init: CBQ => Unit) : Future[CheckboxAnswer] = scheduleScalar(CBQFactory(), init)
  def CheckboxDistributionQuestion(init: CBDQ => Unit) : Future[Set[CheckboxAnswer]] = scheduleVector(CBDQFactory(), init)
  def FreeTextQuestion(init: FTQ => Unit) : Future[FreeTextAnswer] = scheduleScalar(FTQFactory(), init)
  def FreeTextDistributionQuestion(init: FTDQ => Unit) : Future[Set[FreeTextAnswer]] = scheduleVector(FTDQFactory(), init)
  def RadioButtonQuestion(init: RBQ => Unit) : Future[RadioButtonAnswer] = scheduleScalar(RBQFactory(), init)
  def RadioButtonDistributionQuestion(init: RBDQ => Unit) : Future[Set[RadioButtonAnswer]] = scheduleVector(RBDQFactory(), init)
  def Option(id: Symbol, text: String) : QuestionOption

  // state management
  protected[automan] def init() {
    plugins_init()
    if (_use_memoization) {
      memo_init()
      thunklog_init()
    }
  }
  protected[automan] def close() = {
    plugins_shutdown()
  }
  private def plugins_init() {
    // load user-supplied plugins using reflection
    _plugins_initialized = _plugins.map { clazz =>
      val instance = clazz.newInstance()
      instance.startup(this)
      instance
    }
  }
  private def plugins_shutdown(): Unit = {
    _plugins_initialized.foreach { plugin => plugin.shutdown() }
  }
  private def memo_init() {
    _memoizer = Some(new AutomanMemoizer(_memo_conn_string, _memo_user, _memo_pass))
  }
  private def thunklog_init() {
    _thunklog = Some(new ThunkLogger(_thunk_conn_string, _thunk_user, _thunk_pass))
  }
  def state_snapshot(): StateInfo = {
    StateInfo(budget, _schedulers.flatMap { s => s.state })
  }

  // Global backend config
  protected[automan] def budget_formatted = {
    val dbudget = _default_budget.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
    val nf = NumberFormat.getCurrencyInstance(_locale)
    nf.setMinimumFractionDigits(1)
    nf.setMaximumFractionDigits(2)
    nf.format(dbudget.doubleValue())
  }

  protected[automan] def get_budget_from_backend(): BigDecimal
  private def scheduleScalar[Q <: Question,A <: Answer](q: Q, init: Q => Unit): Future[A] = Future {
    init(q)
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    _schedulers = sched :: _schedulers
    sched.run().asInstanceOf[A]
  }
  private def scheduleVector[Q <: Question,A <: Answer](q: Q, init: Q => Unit): Future[Set[A]] = Future {
    init(q)
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    _schedulers = sched :: _schedulers
    sched.run().asInstanceOf[Set[A]]
  }

  // subclass instantiators; these are needed because
  // the JVM erases our type parameters (RBQ) at runtime
  // and thus 'new RBQ' does not suffice in the DSL call above
  protected def CBQFactory() : CBQ
  protected def CBDQFactory() : CBDQ
  protected def FTQFactory() : FTQ
  protected def FTDQFactory() : FTDQ
  protected def RBQFactory() : RBQ
  protected def RBDQFactory() : RBDQ

}