package org.automanlang.core

import java.util.{UUID, Date, Locale}
import org.automanlang.core.answer.Outcome
import org.automanlang.core.logging.LogConfig.LogConfig
import org.automanlang.core.question._
import org.automanlang.core.logging._
import org.automanlang.core.scheduler.SchedulerState.SchedulerState
import org.automanlang.core.scheduler.{SchedulerState, Task}

abstract class AutomanAdapter {
  // question types are determined by adapter implementations
  // answer types are invariant
  type CBQ    <: CheckboxQuestion           // answer scalar
  type CBDQ   <: CheckboxVectorQuestion     // answer vector
  type MEQ    <: MultiEstimationQuestion    // answer multi-estimate
  type EQ     <: EstimationQuestion         // answer estimate
  type FTQ    <: FreeTextQuestion           // answer scalar
  type FTDQ   <: FreeTextVectorQuestion     // answer vector
  type RBQ    <: RadioButtonQuestion        // answer scalar
  type RBDQ   <: RadioButtonVectorQuestion  // answer vector
  type MemoDB <: Memo

  protected var _database_path: String = "AutoManMemoDB_" + UUID.randomUUID()
  protected var _default_confidence: Double = 0.95
  protected var _in_mem_db: Boolean = false
  protected var _locale: Locale = Locale.getDefault
  protected var _log_config: LogConfig = LogConfig.TRACE_MEMOIZE_VERBOSE
  protected var _memoizer: MemoDB = _
  protected var _plugins: List[Class[_ <: Plugin]] = List.empty
  protected var _plugins_initialized: List[_ <: Plugin] = List.empty
  // _ref_cache invariant: newest outcome is always at the head of the list;
  //                       every Outcome.answer is composed with the Outcome.answer
  //                       before it (tailward)
  protected var _ref_cache: Map[String, List[Outcome[_]]] = Map()

  // user-visible getters and setters
  def database_path: String = _database_path
  def database_path_=(path: String) = { _database_path = path }
  def default_confidence: Double = _default_confidence
  def default_confidence_=(c: Double) { _default_confidence = c }
  def in_memory_db: Boolean = _in_mem_db
  def in_memory_db_=(use_memdb: Boolean) { _in_mem_db = use_memdb }
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
  protected[automanlang] def accept(ts: List[Task]) : Option[List[Task]]

  /**
    * Get the budget from the backend.
    * @return Some budget if successful.
    */
  protected[automanlang] def backend_budget(): Option[BigDecimal]

  /**
    * Cancel the given tasks.
    * @param ts A list of tasks to cancel.
    * @param toState Which scheduler state tasks should become after cancellation.
    * @return Some list of cancelled tasks if successful.
    */
  protected[automanlang] def cancel(ts: List[Task], toState: SchedulerState.Value) : Option[List[Task]]

  /**
   * Post tasks on the backend, one task for each task.  All tasks given should
   * be marked READY. The method returns the complete list of tasks passed
   * but with new states. Blocking. Invariant: the size of the list of input
   * tasks == the size of the list of the output tasks.
   * @param ts A list of new tasks.
   * @param exclude_worker_ids Worker IDs to exclude, if any.
   * @return Some list of the posted tasks if successful.
   */
  protected[automanlang] def post(ts: List[Task], exclude_worker_ids: List[String]) : Option[List[Task]]

  /**
   * Tell the backend to reject the answer associated with this ANSWERED task.
   * @param ts_reasons A list of pairs of ANSWERED tasks and their rejection reasons.
   * @return Some REJECTED tasks if succesful.
   */
  protected[automanlang] def reject(ts_reasons: List[(Task,String)]) : Option[List[Task]]

  /**
   * Ask the backend to retrieve answers given a list of RUNNING tasks. Invariant:
   * the size of the list of input tasks == the size of the list of the output
   * tasks. The virtual_time parameter is ignored when not running in simulator mode.
   * @param ts A list of RUNNING tasks.
   * @param current_time The current virtual time.
   * @return Some list of RUNNING, RETRIEVED, or TIMEOUT tasks if successful.
   */
  protected[automanlang] def retrieve(ts: List[Task], current_time: Date) : Option[List[Task]]

  /**
   * This method is called by the scheduler after question initialization
   * but before any tasks are scheduled. Override it to provide a
   * backend-specific startup implementation.
   * @param q Question
   * @param t Scheduler startup time
   */
  protected[automanlang] def question_startup_hook(q: Question, t: Date): Unit = {
    q.questionStartupHook()
  }
  /**
   * This method is called by the scheduler after an answer has been
   * accepted by the scheduler policy. Override it to provide a
   * backend-specific shutdown implementation.
   * @param q Question
   */
  protected[automanlang] def question_shutdown_hook(q: Question): Unit = {
    q.questionShutdownHook()
  }

  // User API
  def CheckboxQuestion(init: CBQ => Unit) = schedule(CBQFactory(), init)
  def CheckboxDistributionQuestion(init: CBDQ => Unit) = schedule(CBDQFactory(), init)
  def MultiEstimationQuestion(init: MEQ => Unit) = schedule(MEQFactory(), init)
  def EstimationQuestion(init: EQ => Unit) = schedule(EQFactory(), init)
  def FreeTextQuestion(init: FTQ => Unit) = schedule(FTQFactory(), init)
  def FreeTextDistributionQuestion(init: FTDQ => Unit) = schedule(FTDQFactory(), init)
  def RadioButtonQuestion(init: RBQ => Unit) = schedule(RBQFactory(), init)
  def RadioButtonDistributionQuestion(init: RBDQ => Unit) = schedule(RBDQFactory(), init)
  def Option(id: Symbol, text: String) : QuestionOption

  def Survey(init: FakeSurvey => Unit): FakeSurvey#O = schedule(SurveyFactory(), init)

  def CreateRadioButtonQuestion(init: RBQ => Unit): RBQ = {
    var q = RBQFactory()
    init(q)
    q
  }
  def CreateCheckboxQuestion(init: CBQ => Unit): CBQ = {
    var q = CBQFactory()
    init(q)
    q
  }
  def CreateEstimateQuestion(init: EQ => Unit): EQ = {
    var q = EQFactory()
    init(q)
    q
  }
  def CreateFreeTextQuestion(init: FTQ => Unit): FTQ = {
    var q = FTQFactory()
    init(q)
    q
  }

  // state management
  protected[automanlang] def close() = {
    plugins_shutdown()
  }
  protected[automanlang] def init() {
    memo_init()
    plugins_init()
    plugins_memo_register()
  }
  protected[automanlang] def plugins_init() {
    // load user-supplied plugins using reflection
    _plugins_initialized = _plugins.map { clazz =>
      val instance = clazz.newInstance()
      instance.startup(this)
      instance
    }
  }
  protected[automanlang] def plugins_memo_register(): Unit = {
    _memoizer.register_plugins(_plugins_initialized)
  }
  protected[automanlang] def plugins_shutdown(): Unit = {
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
  protected[automanlang] def memo_restore(q: Question) : List[Task] = {
    _memoizer.restore(q)
  }
  def state_snapshot(): List[TaskSnapshot[_]] = {
    _memoizer.snapshot()
  }

  /**
    * This function initializes a Question object of the appropriate type
    * and wraps it in an Outcome monad.  It does NOT schedule associated
    * tasks; this function should be used for questions nested within
    * a survey.
    * @param q The question.
    * @param init An initializer function for the question.
    * @tparam Q The type of the question.
    * @return An Outcome.
    */
  protected[automanlang] def noschedule[Q <: Question](q: Q, init: Q => Unit): Q#O = {
    init(q)
    q.getOutcome(this)
  }

  /**
    * This function initializes a Question object of the appropriate type
    * and wraps it in an Outcome monad.  As a side effect, it starts a
    * scheduler and proceeds to marshal the task to the backend.
    * @param q The question.
    * @param init An initializer function for the question.
    * @tparam Q The type of the question.
    * @return An Outcome.
    */
  protected[automanlang] def schedule[Q <: Question](q: Q, init: Q => Unit): Q#O = {
    // initialize question with end-user lambda
    // memo hash cannot be calculated correctly until Question has been initialized
    init(q)

    val memo_hash = q.memo_hash

    // first check reference cache (to ensure ref. transparency)
    _ref_cache.synchronized {
      if (_ref_cache.contains(memo_hash)) {
        // get the last requested Outcome
        val prev = _ref_cache(memo_hash).head.asInstanceOf[q.O]

        // compose it with our new Outcome
        val o = q.composeOutcome(prev, this)

        // prepend Outcome to list
        _ref_cache += memo_hash -> (o :: _ref_cache(memo_hash))

        // return
        o
      } else {
        // get Outcome
        val o = q.getOutcome(this)

        // put Outcome into cache
        _ref_cache += memo_hash -> List(o)

        // return Outcome
        o
      }
    }
  }

  // subclass instantiators; these are needed because
  // the JVM erases our type parameters (RBQ) at runtime
  // and thus 'new RBQ' does not suffice in the DSL call above
  protected def CBQFactory() : CBQ
  protected def CBDQFactory() : CBDQ
  protected def MEQFactory() : MEQ
  protected def EQFactory() : EQ
  protected def FTQFactory() : FTQ
  protected def FTDQFactory() : FTDQ
  protected def RBQFactory() : RBQ
  protected def RBDQFactory() : RBDQ
  protected def SurveyFactory() : FakeSurvey
  protected def MemoDBFactory() : MemoDB
}
