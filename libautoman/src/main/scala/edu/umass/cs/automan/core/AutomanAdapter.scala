package edu.umass.cs.automan.core

import java.util.{UUID, Date, Locale}
import edu.umass.cs.automan.core.logging.LogConfig.LogConfig
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.scheduler.Task

abstract class AutomanAdapter {
  // question types are determined by adapter implementations
  // answer types are invariant
  type CBQ    <: CheckboxQuestion           // answer scalar
  type CBDQ   <: CheckboxVectorQuestion     // answer vector
  type EQ     <: EstimationQuestion         // answer estimate
  type FTQ    <: FreeTextQuestion           // answer scalar
  type FTDQ   <: FreeTextVectorQuestion     // answer vector
  type RBQ    <: RadioButtonQuestion        // answer scalar
  type RBDQ   <: RadioButtonVectorQuestion  // answer vector
  type MemoDB <: Memo

  protected var _database_path: String = "AutoManMemoDB_" + UUID.randomUUID()
  protected var _default_confidence: Double = 0.95
  protected var _locale: Locale = Locale.getDefault
  protected var _log_config: LogConfig = LogConfig.TRACE_MEMOIZE_VERBOSE
  protected var _memoizer: MemoDB = _
  protected var _plugins: List[Class[_ <: Plugin]] = List.empty
  protected var _plugins_initialized: List[_ <: Plugin] = List.empty

  // user-visible getters and setters
  def database_path: String = _database_path
  def database_path_=(path: String) = { _database_path = path }
  def default_confidence: Double = _default_confidence
  def default_confidence_=(c: Double) { _default_confidence = c }
  def plugins: List[Class[_ <: Plugin]] = _plugins
  def plugins_=(ps: List[Class[_ <: Plugin]]) { _plugins = ps }
  def logging = _log_config
  def logging_=(lc: LogConfig.Value) { _log_config = lc }
  def log_verbosity = DebugLog.level
  def log_verbosity_=(v: LogLevel) { DebugLog.level = v }

  // marshaling calls
  // invariants:
  //  1. every task that is passed in is passed back with modified state.
  //  2. return values are optional: None signals fatal backend failure.
  /**
   * Tell the backend to accept the answer associated with this ANSWERED task.
   * @param ts ANSWERED tasks.
   * @return Some ACCEPTED tasks if successful.
   */
  protected[automan] def accept(ts: List[Task]) : Option[List[Task]]

  /**
    * Get the budget from the backend.
    * @return Some budget if successful.
    */
  protected[automan] def backend_budget(): Option[BigDecimal]

  /**
    * Cancel the given tasks.
    * @param ts A list of tasks to cancel.
    * @return Some list of cancelled tasks if successful.
    */
  protected[automan] def cancel(ts: List[Task]) : Option[List[Task]]

  /**
   * Post tasks on the backend, one task for each task.  All tasks given should
   * be marked READY. The method returns the complete list of tasks passed
   * but with new states. Blocking. Invariant: the size of the list of input
   * tasks == the size of the list of the output tasks.
   * @param ts A list of new tasks.
   * @param exclude_worker_ids Worker IDs to exclude, if any.
   * @return Some list of the posted tasks if successful.
   */
  protected[automan] def post(ts: List[Task], exclude_worker_ids: List[String]) : Option[List[Task]]

  /**
   * Tell the backend to reject the answer associated with this ANSWERED task.
   * @param ts_reasons A list of pairs of ANSWERED tasks and their rejection reasons.
   * @return Some REJECTED tasks if succesful.
   */
  protected[automan] def reject(ts_reasons: List[(Task,String)]) : Option[List[Task]]

  /**
   * Ask the backend to retrieve answers given a list of RUNNING tasks. Invariant:
   * the size of the list of input tasks == the size of the list of the output
   * tasks. The virtual_time parameter is ignored when not running in simulator mode.
   * @param ts A list of RUNNING tasks.
   * @param current_time The current virtual time.
   * @return Some list of RUNNING, RETRIEVED, or TIMEOUT tasks if successful.
   */
  protected[automan] def retrieve(ts: List[Task], current_time: Date) : Option[List[Task]]

  /**
   * This method is called by the scheduler after question initialization
   * but before any tasks are scheduled. Override it to provide a
   * backend-specific startup implementation.
   * @param q Question
   * @param t Scheduler startup time
   */
  protected[automan] def question_startup_hook(q: Question, t: Date): Unit = {
    q.questionStartupHook()
  }
  /**
   * This method is called by the scheduler after an answer has been
   * accepted by the scheduler policy. Override it to provide a
   * backend-specific shutdown implementation.
   * @param q Question
   */
  protected[automan] def question_shutdown_hook(q: Question): Unit = {
    q.questionShutdownHook()
  }

  // User API
  def CheckboxQuestion(init: CBQ => Unit) = schedule(CBQFactory(), init)
  def CheckboxDistributionQuestion(init: CBDQ => Unit) = schedule(CBDQFactory(), init)
  def EstimationQuestion(init: EQ => Unit) = schedule(EQFactory(), init)
  def FreeTextQuestion(init: FTQ => Unit) = schedule(FTQFactory(), init)
  def FreeTextDistributionQuestion(init: FTDQ => Unit) = schedule(FTDQFactory(), init)
  def RadioButtonQuestion(init: RBQ => Unit) = schedule(RBQFactory(), init)
  def RadioButtonDistributionQuestion(init: RBDQ => Unit) = schedule(RBDQFactory(), init)
  def Option(id: Symbol, text: String) : QuestionOption

  // state management
  protected[automan] def close() = {
    plugins_shutdown()
  }
  protected[automan] def init() {
    memo_init()
    plugins_init()
    plugins_memo_register()
  }
  protected[automan] def plugins_init() {
    // load user-supplied plugins using reflection
    _plugins_initialized = _plugins.map { clazz =>
      val instance = clazz.newInstance()
      instance.startup(this)
      instance
    }
  }
  protected[automan] def plugins_memo_register(): Unit = {
    _memoizer.register_plugins(_plugins_initialized)
  }
  protected[automan] def plugins_shutdown(): Unit = {
    _plugins_initialized.foreach { plugin => plugin.shutdown() }
  }
  def memo_init() {
    _memoizer = MemoDBFactory()
    _memoizer.init()
  }
  def memo_delete(): Unit = {
    if (_memoizer != null) {
      _memoizer.wipeDatabase()
    }
  }
  protected[automan] def memo_restore(q: Question) : List[Task] = {
    _memoizer.restore(q)
  }
  def state_snapshot(): List[TaskSnapshot[_]] = {
    _memoizer.snapshot()
  }

  // thread management
  private def schedule[Q <: Question](q: Q, init: Q => Unit): Q#O = {
    // initialize question with end-user lambda
    init(q)
    // start job
    q.getOutcome(this)
  }

  // subclass instantiators; these are needed because
  // the JVM erases our type parameters (RBQ) at runtime
  // and thus 'new RBQ' does not suffice in the DSL call above
  protected def CBQFactory() : CBQ
  protected def CBDQFactory() : CBDQ
  protected def EQFactory() : EQ
  protected def FTQFactory() : FTQ
  protected def FTDQFactory() : FTDQ
  protected def RBQFactory() : RBQ
  protected def RBDQFactory() : RBDQ
  protected def MemoDBFactory() : MemoDB
}
