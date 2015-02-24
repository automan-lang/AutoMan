package edu.umass.cs.automan.core

import java.util.Locale
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.info.StateInfo
import edu.umass.cs.automan.core.logging.LogConfig.LogConfig
import edu.umass.cs.automan.core.logging.LogConfig.LogConfig
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.logging.{LogConfig, Memo}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Scheduler, Thunk}
import scala.concurrent.{blocking, Future}

abstract class AutomanAdapter {
  // question types are determined by adapter implementations
  // answer types are invariant
//  type CBQ <: CheckboxQuestion                    // answer scalar
//  type CBDQ <: CheckboxDistributionQuestion       // answer vector
//  type FTQ <: FreeTextQuestion                    // answer scalar
//  type FTDQ <: FreeTextDistributionQuestion       // answer vector
  type RBQ  <: RadioButtonQuestion                 // answer scalar
//  type RBDQ <: RadioButtonDistributionQuestion    // answer vector

  protected var _default_confidence: Double = 0.95
  protected var _locale: Locale = Locale.getDefault
  protected var _memoizer: Memo = _
  protected var _plugins: List[Class[_ <: Plugin]] = List.empty
  protected var _plugins_initialized: List[_ <: Plugin] = List.empty
  protected var _poll_interval_in_s : Int = 30
  protected var _schedulers: List[Scheduler[_]] = List.empty
  protected var _thunk_db: String = "ThunkLogDB"
  protected var _thunk_conn_string: String = "jdbc:derby:" + _thunk_db + ";create=true"
  protected var _thunk_user: String = ""
  protected var _thunk_pass: String = ""
  protected var _log_config: LogConfig = LogConfig.TRACE_MEMOIZE_VERBOSE

  // user-visible getters and setters
  def default_confidence: Double = _default_confidence
  def default_confidence_=(c: Double) { _default_confidence = c }
  def plugins: List[Class[_ <: Plugin]] = _plugins
  def plugins_=(ps: List[Class[_ <: Plugin]]) { _plugins = ps }
  def logging = _log_config
  def logging_=(lc: LogConfig.Value) { _log_config = lc }

  // marshaling calls
  // invariant: every Thunk that is passed in is passed back
  protected[automan] def accept[A](t: Thunk[A]) : Thunk[A]
  protected[automan] def backend_budget(): BigDecimal
  protected[automan] def cancel[A](t: Thunk[A]) : Thunk[A]
  /**
   * Post tasks on the backend, one task for each Thunk.  All Thunks given should
   * be marked READY. The method returns the complete list of Thunks passed
   * but with new states. Nonblocking. Invariant: the size of the list of input
   * Thunks == the size of the list of the output Thunks.
   * @param ts A list of new Thunks.
   * @param exclude_worker_ids Worker IDs to exclude, if any.
   * @tparam A The data type of the Answer value.
   * @return A list of the posted Thunks.
   */
  protected[automan] def post[A](ts: List[Thunk[A]], exclude_worker_ids: List[String]) : List[Thunk[A]]
  protected[automan] def process_custom_info[A](t: Thunk[A], i: Option[String]) : Thunk[A]
  protected[automan] def reject[A](t: Thunk[A]) : Thunk[A]

  /**
   * Ask the backend to retrieve answers given a list of RUNNING Thunks. Invariant:
   * the size of the list of input Thunks == the size of the list of the output
   * Thunks.
   * @param ts A list of RUNNING thunks.
   * @tparam A The data type of the Answer value.
   * @return A list of RUNNING, RETRIEVED, or TIMEOUT Thunks.
   */
  protected[automan] def retrieve[A](ts: List[Thunk[A]]) : List[Thunk[A]]
//  protected[automan] def timeout[A <: Answer](ts: List[Thunk[A]]) : List[Thunk[A]]
  protected[automan] def question_startup_hook[A](q: Question[A]): Unit = {}
  protected[automan] def question_shutdown_hook[A](q: Question[A]): Unit = {}

  // end-user syntax: Question creation
//  def CheckboxQuestion(init: CBQ => Unit) : Future[CheckboxOldAnswer] = schedule(CBQFactory(), init)
//  def CheckboxDistributionQuestion(init: CBDQ => Unit) : Future[Set[CheckboxOldAnswer]] = scheduleVector(CBDQFactory(), init)
//  def FreeTextQuestion(init: FTQ => Unit) : Future[FreeTextOldAnswer] = schedule(FTQFactory(), init)
//  def FreeTextDistributionQuestion(init: FTDQ => Unit) : Future[Set[FreeTextOldAnswer]] = scheduleVector(FTDQFactory(), init)
  def RadioButtonQuestion(init: Question[Symbol] => Unit) = schedule(RBQFactory(), init)
//  def RadioButtonDistributionQuestion(init: RBDQ => Unit) : Future[Set[RadioButtonOldAnswer]] = scheduleVector(RBDQFactory(), init)
  def Option(id: Symbol, text: String) : QuestionOption

  // state management
  protected[automan] def init() {
    plugins_init()
    memo_init()
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
  protected[automan] def memo_init() {
    _memoizer = new Memo(_log_config)
  }

//  def state_snapshot(): StateInfo = {
//    StateInfo(_schedulers.flatMap { s => s.state })
//  }

  // thread management
  private def schedule[A](q: Question[A], init: Question[A] => Unit): Answer[A] = {
    // initialize question with end-user lambda
    init(q)
    // initialize QA strategy
    q.init_strategy()
    // startup a new scheduler
    val sched = new Scheduler(q, this, _memoizer, _poll_interval_in_s)
    // add scheduler to list for plugin inspection
    _schedulers = sched :: _schedulers
    // start job
    q.getAnswer(sched)
  }


  // subclass instantiators; these are needed because
  // the JVM erases our type parameters (RBQ) at runtime
  // and thus 'new RBQ' does not suffice in the DSL call above
//  protected def CBQFactory() : CBQ
//  protected def CBDQFactory() : CBDQ
//  protected def FTQFactory() : FTQ
//  protected def FTDQFactory() : FTDQ
  protected def RBQFactory() : RBQ
//  protected def RBDQFactory() : RBDQ

}