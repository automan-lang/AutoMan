package edu.umass.cs.automan.core

import java.util.Date

import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.question.{CheckboxQuestion, CheckboxVectorQuestion, EstimationQuestion, FreeTextQuestion, FreeTextVectorQuestion, MultiEstimationQuestion, QuestionOption, RadioButtonQuestion, RadioButtonVectorQuestion, Survey}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

abstract class NoOpAdapter extends AutomanAdapter {
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
  type S      <: Survey

  /**
    * Tell the backend to accept the answer associated with this ANSWERED task.
    *
    * @param ts ANSWERED tasks.
    * @return Some ACCEPTED tasks if successful.
    */
  override protected[automan] def accept(ts: List[Task]): Option[List[Task]] = None

  /**
    * Get the budget from the backend.
    *
    * @return Some budget if successful.
    */
  override protected[automan] def backend_budget(): Option[BigDecimal] = None

  /**
    * Cancel the given tasks.
    *
    * @param ts      A list of tasks to cancel.
    * @param toState Which scheduler state tasks should become after cancellation.
    * @return Some list of cancelled tasks if successful.
    */
override protected[automan] def cancel(ts: List[Task], toState: SchedulerState.Value): Option[List[Task]] = None

  /**
    * Post tasks on the backend, one task for each task.  All tasks given should
    * be marked READY. The method returns the complete list of tasks passed
    * but with new states. Blocking. Invariant: the size of the list of input
    * tasks == the size of the list of the output tasks.
    *
    * @param ts                 A list of new tasks.
    * @param exclude_worker_ids Worker IDs to exclude, if any.
    * @return Some list of the posted tasks if successful.
    */
override protected[automan] def post(ts: List[Task], exclude_worker_ids: List[String]): Option[List[Task]] = None

  /**
    * Tell the backend to reject the answer associated with this ANSWERED task.
    *
    * @param ts_reasons A list of pairs of ANSWERED tasks and their rejection reasons.
    * @return Some REJECTED tasks if succesful.
   */
override protected[automan] def reject(ts_reasons: List[(Task, String)]): Option[List[Task]] = None

  /**
    * Ask the backend to retrieve answers given a list of RUNNING tasks. Invariant:
    * the size of the list of input tasks == the size of the list of the output
    * tasks. The virtual_time parameter is ignored when not running in simulator mode.
    *
    * @param ts           A list of RUNNING tasks.
    * @param current_time The current virtual time.
    * @return Some list of RUNNING, RETRIEVED, or TIMEOUT tasks if successful.
   */
override protected[automan] def retrieve(ts: List[Task], current_time: Date): Option[List[Task]] = None

//override def Option(id: Symbol, text: String): QuestionOption = { super.Option(id, text)}
//
//override protected def CBQFactory(): CBQ = { super.CBQFactory() }
//override protected def CBDQFactory(): CBDQ = { super.CBDQFactory() }
//override protected def MEQFactory(): MEQ = { super.MEQFactory() }
//override protected def EQFactory(): EQ = { super.EQFactory() }
//override protected def FTQFactory(): FTQ = { super.FTQFactory() }
//override protected def FTDQFactory(): FTDQ = { super.FTDQFactory() }
//override protected def RBQFactory(): RBQ = { super.RBQFactory() }
//override protected def RBDQFactory(): RBDQ = { super.RBDQFactory() }
//override protected def MemoDBFactory(): MemoDB = { super.MemoDBFactory() }
//override protected def SFactory(): S = { super.SFactory() }
}
