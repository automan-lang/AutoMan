package edu.umass.cs.automan.core

import java.util.Locale
import edu.umass.cs.automan.core.answer.Outcome
import edu.umass.cs.automan.core.logging.LogConfig.LogConfig
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.logging.{LogConfig, Memo}
import edu.umass.cs.automan.core.scheduler.{Scheduler, Thunk}

abstract class AutomanAdapter {
  // question types are determined by adapter implementations
  // answer types are invariant
  type CBQ <: CheckboxQuestion                    // answer scalar
//  type CBDQ <: CheckboxDistributionQuestion       // answer vector
  type FTQ <: FreeTextQuestion                    // answer scalar
//  type FTDQ <: FreeTextDistributionQuestion       // answer vector
  type RBQ  <: RadioButtonQuestion                 // answer scalar
//  type RBDQ <: RadioButtonDistributionQuestion    // answer vector
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
  // invariant: every Thunk that is passed in is passed back
  /**
   * Tell the backend to accept the answer associated with this ANSWERED Thunk.
   * @param t An ANSWERED Thunk.
   * @return An ACCEPTED Thunk.
   */
  protected[automan] def accept(t: Thunk) : Thunk
  protected[automan] def backend_budget(): BigDecimal
  protected[automan] def cancel(t: Thunk) : Thunk
  /**
   * Post tasks on the backend, one task for each Thunk.  All Thunks given should
   * be marked READY. The method returns the complete list of Thunks passed
   * but with new states. Nonblocking. Invariant: the size of the list of input
   * Thunks == the size of the list of the output Thunks.
   * @param ts A list of new Thunks.
   * @param exclude_worker_ids Worker IDs to exclude, if any.
   * @return A list of the posted Thunks.
   */
  protected[automan] def post(ts: List[Thunk], exclude_worker_ids: List[String]) : List[Thunk]

  /**
   * Tell the backend to reject the answer associated with this ANSWERED Thunk.
   * @param t An ANSWERED Thunk.
   * @param rejection_response Reason for rejection, e.g., the correct answer was different.
   * @return A REJECTED Thunk.
   */
  protected[automan] def reject(t: Thunk, rejection_response: String) : Thunk

  /**
   * Ask the backend to retrieve answers given a list of RUNNING Thunks. Invariant:
   * the size of the list of input Thunks == the size of the list of the output
   * Thunks.
   * @param ts A list of RUNNING thunks.
   * @return A list of RUNNING, RETRIEVED, or TIMEOUT Thunks.
   */
  protected[automan] def retrieve(ts: List[Thunk]) : List[Thunk]

  /**
   * This method is called by the scheduler after question initialization
   * but before any tasks are scheduled. Override it to provide a
   * backend-specific startup implementation.
   * @param q Question
   */
  protected[automan] def question_startup_hook(q: Question): Unit = {}
  /**
   * This method is called by the scheduler after an answer has been
   * accepted by the scheduler policy. Override it to provide a
   * backend-specific shutdown implementation.
   * @param q Question
   */
  protected[automan] def question_shutdown_hook(q: Question): Unit = {}

  // end-user syntax: Question creation
 def CheckboxQuestion(init: CBQ => Unit) = schedule(CBQFactory(), init)
//  def CheckboxDistributionQuestion(init: CBDQ => Unit) : Future[Set[CheckboxOldAnswer]] = scheduleVector(CBDQFactory(), init)
  def FreeTextQuestion(init: FTQ => Unit) = schedule(FTQFactory(), init)
//  def FreeTextDistributionQuestion(init: FTDQ => Unit) : Future[Set[FreeTextOldAnswer]] = scheduleVector(FTDQFactory(), init)
  def RadioButtonQuestion(init: RBQ => Unit) = schedule(RBQFactory(), init)
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
    _memoizer = MemoDBFactory()
    _memoizer.init()
  }

  // thread management
  private def schedule[Q <: Question](q: Q, init: Q => Unit): Q#O = {
    // initialize question with end-user lambda
    init(q)
    // initialize QA strategy
    q.init_strategy()
    // start job
    q.getOutcome(this, _memoizer, _poll_interval_in_s)
  }

  // subclass instantiators; these are needed because
  // the JVM erases our type parameters (RBQ) at runtime
  // and thus 'new RBQ' does not suffice in the DSL call above
  protected def CBQFactory() : CBQ
//  protected def CBDQFactory() : CBDQ
  protected def FTQFactory() : FTQ
//  protected def FTDQFactory() : FTDQ
  protected def RBQFactory() : RBQ
//  protected def RBDQFactory() : RBDQ
  protected def MemoDBFactory() : MemoDB

}