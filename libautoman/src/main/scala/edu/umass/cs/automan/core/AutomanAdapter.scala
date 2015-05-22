package edu.umass.cs.automan.core

import java.util.Locale
import edu.umass.cs.automan.core.logging.LogConfig.LogConfig
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.logging.{LogConfig, Memo}
import edu.umass.cs.automan.core.scheduler.{Scheduler, Task}

abstract class AutomanAdapter {
  // question types are determined by adapter implementations
  // answer types are invariant
  type CBQ    <: CheckboxQuestion                 // answer scalar
  type CBDQ   <: CheckboxDistributionQuestion     // answer vector
  type FTQ    <: FreeTextQuestion                 // answer scalar
  type FTDQ   <: FreeTextDistributionQuestion     // answer vector
  type RBQ    <: RadioButtonQuestion              // answer scalar
  type RBDQ   <: RadioButtonDistributionQuestion  // answer vector
  type MemoDB <: Memo

  protected var _default_confidence: Double = 0.95
  protected var _locale: Locale = Locale.getDefault
  protected var _log_config: LogConfig = LogConfig.TRACE_MEMOIZE_VERBOSE
  protected var _memoizer: MemoDB = _
  protected var _plugins: List[Class[_ <: Plugin]] = List.empty
  protected var _plugins_initialized: List[_ <: Plugin] = List.empty
  protected var _poll_interval_in_s : Int = 30

  // user-visible getters and setters
  def default_confidence: Double = _default_confidence
  def default_confidence_=(c: Double) { _default_confidence = c }
  def plugins: List[Class[_ <: Plugin]] = _plugins
  def plugins_=(ps: List[Class[_ <: Plugin]]) { _plugins = ps }
  def logging = _log_config
  def logging_=(lc: LogConfig.Value) { _log_config = lc }

  // marshaling calls
  // invariant: every task that is passed in is passed back
  /**
   * Tell the backend to accept the answer associated with this ANSWERED task.
   * @param t An ANSWERED task.
   * @return An ACCEPTED task.
   */
  protected[automan] def accept(t: Task) : Task
  protected[automan] def backend_budget(): BigDecimal
  protected[automan] def cancel(t: Task) : Task
  /**
   * Post tasks on the backend, one task for each task.  All tasks given should
   * be marked READY. The method returns the complete list of tasks passed
   * but with new states. Nonblocking. Invariant: the size of the list of input
   * tasks == the size of the list of the output tasks.
   * @param ts A list of new tasks.
   * @param exclude_worker_ids Worker IDs to exclude, if any.
   * @return A list of the posted tasks.
   */
  protected[automan] def post(ts: List[Task], exclude_worker_ids: List[String]) : List[Task]

  /**
   * Tell the backend to reject the answer associated with this ANSWERED task.
   * @param t An ANSWERED task.
   * @param rejection_response Reason for rejection, e.g., the correct answer was different.
   * @return A REJECTED task.
   */
  protected[automan] def reject(t: Task, rejection_response: String) : Task

  /**
   * Ask the backend to retrieve answers given a list of RUNNING tasks. Invariant:
   * the size of the list of input tasks == the size of the list of the output
   * tasks.
   * @param ts A list of RUNNING tasks.
   * @return A list of RUNNING, RETRIEVED, or TIMEOUT tasks.
   */
  protected[automan] def retrieve(ts: List[Task]) : List[Task]

  /**
   * This method is called by the scheduler after question initialization
   * but before any tasks are scheduled. Override it to provide a
   * backend-specific startup implementation.
   * @param q Question
   */
  protected[automan] def question_startup_hook(q: Question): Unit = {
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
  def FreeTextQuestion(init: FTQ => Unit) = schedule(FTQFactory(), init)
  def FreeTextDistributionQuestion(init: FTDQ => Unit) = schedule(FTDQFactory(), init)
  def RadioButtonQuestion(init: RBQ => Unit) = schedule(RBQFactory(), init)
  def RadioButtonDistributionQuestion(init: RBDQ => Unit) = schedule(RBDQFactory(), init)
  def Option(id: Symbol, text: String) : QuestionOption
  def clearMemoDB(): Unit = {
    if (_memoizer != null) {
      _memoizer.wipeDatabase()
    }
  }

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
    _memoizer = MemoDBFactory()
    _memoizer.init()
  }

  // thread management
  private def schedule[Q <: Question](q: Q, init: Q => Unit): Q#O = {
    // initialize question with end-user lambda
    init(q)
    // start job
    q.getOutcome(this, _memoizer, _poll_interval_in_s)
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
  protected def MemoDBFactory() : MemoDB
}