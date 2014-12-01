package edu.umass.cs.automan.core

import java.text.NumberFormat

import akka.io.IO
import edu.umass.cs.automan.core.debugger.{Tasks, DebugServer}
import spray.can.Http
import akka.pattern.ask
import scala.concurrent.duration._
import java.util.Locale
import akka.actor.{ActorRef, Props, ActorSystem}
import edu.umass.cs.automan.core.question._
import answer._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import memoizer.{ThunkLogger, AutomanMemoizer}
import edu.umass.cs.automan.core.scheduler.{Scheduler, Thunk}

abstract class AutomanAdapter {
  // the precise question type is determined by the subclassing
  // adapter; answer types are invariant
  type RBQ <: RadioButtonQuestion                 // answer scalar
  type RBDQ <: RadioButtonDistributionQuestion    // answer vector
  type CBQ <: CheckboxQuestion                    // answer scalar
  type FTQ <: FreeTextQuestion                    // answer scalar
  protected implicit var _actor_system: ActorSystem = _
  protected var _default_budget: BigDecimal = 0.00
  protected var _default_confidence: Double = 0.95
  protected var _debug_mode: Boolean = false
  protected var _debugger_actor: ActorRef = _
  protected var _locale: Locale = Locale.getDefault
  protected var _memoizer: AutomanMemoizer = _
  protected var _memo_db: String = "AutomanMemoDB"
  protected def _memo_conn_string: String = "jdbc:derby:" + _memo_db + ";create=true"
  protected var _memo_user: String = ""
  protected var _memo_pass: String = ""
  protected var _poll_interval_in_s : Int = 30
  protected var _debug_schedulers: List[Scheduler] = List()
  protected var _thunklog: ThunkLogger = _
  protected var _thunk_db: String = "ThunkLogDB"
  protected var _thunk_conn_string: String = "jdbc:derby:" + _thunk_db + ";create=true"
  protected var _thunk_user: String = ""
  protected var _thunk_pass: String = ""

  // user-visible getters and setters
  def default_budget: BigDecimal = _default_budget
  def default_budget_=(b: BigDecimal) { _default_budget = b }
  def default_confidence: Double = _default_confidence
  def default_confidence_=(c: Double) { _default_confidence = c }
  def debug: Boolean = _debug_mode
  def debug_=(d: Boolean) = { _debug_mode = d }
  def locale: Locale = _locale
  def locale_=(l: Locale) { _locale = l }

  // marshaling calls
  protected def accept[A <: Answer](t: Thunk[A])
  protected def cancel[A <: Answer](t: Thunk[A])
  protected def post[A <: Answer](ts: List[Thunk[A]], dual: Boolean, exclude_worker_ids: List[String])
  protected def process_custom_info[A <: Answer](t: Thunk[A], i: Option[String])
  protected def reject[A <: Answer](t: Thunk[A])
  protected def retrieve[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]]  // returns all thunks passed in
  protected def question_startup_hook(q: Question): Unit = {}
  protected def question_shutdown_hook(q: Question): Unit = {}

  // end-user syntax: Question creation
  def CheckboxQuestion(init: CBQ => Unit) : Future[CheckboxAnswer] = scheduleScalar(CBQFactory(), init)
  def FreeTextQuestion(init: FTQ => Unit) : Future[FreeTextAnswer] = scheduleScalar(FTQFactory(), init)
  def RadioButtonQuestion(init: RBQ => Unit) : Future[RadioButtonAnswer] = scheduleScalar(RBQFactory(), init)
  def RadioButtonDistributionQuestion(init: RBDQ => Unit) : Future[Set[RadioButtonAnswer]] = scheduleVector(RBDQFactory(), init)
  def Option(id: Symbol, text: String) : QuestionOption

  // state management
  protected def init() {
    debugger_init()
    memo_init()
    thunklog_init()
  }
  protected def close() = {
    if (_debug_mode) {
      _actor_system.shutdown()
    }
  }
  private def debugger_init() {
    if (_debug_mode) {
      // init actor system
      _actor_system = ActorSystem("on-spray-can")

      // actor properties
      val props = Props(new DebugServer(this))

      // init debugger actor
      _debugger_actor = _actor_system.actorOf(props, "debugger-service")

      // set timeout implicit for ? (ask)
      implicit val timeout = akka.util.Timeout(5.seconds)

      // start a new HTTP server on port 8080 with our service actor as the handler
      IO(Http) ? Http.Bind(_debugger_actor, interface = "localhost", port = 8080)
    }
  }
  private def memo_init() {
    _memoizer = new AutomanMemoizer(_memo_conn_string, _memo_user, _memo_pass)
  }
  private def thunklog_init() {
    _thunklog = new ThunkLogger(_thunk_conn_string, _thunk_user, _thunk_pass)
  }

  // Global backend config
  protected def budget_formatted = {
    val dbudget = _default_budget.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
    val nf = NumberFormat.getCurrencyInstance(_locale)
    nf.setMinimumFractionDigits(1)
    nf.setMaximumFractionDigits(2)
    nf.format(dbudget.doubleValue())
  }

  protected def get_budget_from_backend(): BigDecimal
  private def scheduleScalar[Q <: Question,A <: Answer](q: Q, init: Q => Unit): Future[A] = Future {
    init(q)
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    if (_debug_mode) {
      _debug_schedulers = sched :: _debug_schedulers
    }
    sched.run().asInstanceOf[A]
  }
  private def scheduleVector[Q <: Question,A <: Answer](q: Q, init: Q => Unit): Future[Set[A]] = Future {
    init(q)
    q.init_strategy()
    val sched = new Scheduler(q, this, _memoizer, _thunklog, _poll_interval_in_s)
    if (_debug_mode) {
      _debug_schedulers = sched :: _debug_schedulers
    }
    sched.run().asInstanceOf[Set[A]]
  }

  // subclass instantiators; these are needed because
  // the JVM erases our type parameters (RBQ) at runtime
  // and thus 'new RBQ' does not suffice in the DSL call above
  protected def RBQFactory() : RBQ
  protected def CBQFactory() : CBQ
  protected def FTQFactory() : FTQ
  protected def RBDQFactory() : RBDQ
  def debug_info: Tasks
}