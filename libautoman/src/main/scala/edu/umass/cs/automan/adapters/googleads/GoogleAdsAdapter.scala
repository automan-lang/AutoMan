package edu.umass.cs.automan.adapters.googleads

import java.util.Date

import forms.question.{GQuestionOption, _}
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.question.QuestionOption
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}


object GoogleAdsAdapter {
  def apply(init: GoogleAdsAdapter => Unit): GoogleAdsAdapter = ???
}

class GoogleAdsAdapter extends AutomanAdapter {
  override type CBQ = GCheckboxQuestion
  override type CBDQ = GCheckboxVectorQuestion
  override type MEQ = GMultiEstimationQuestion
  override type EQ = GEstimationQuestion
  override type FTQ = GFreeTextQuestion
  override type FTDQ = GFreeTextVectorQuestion
  override type RBQ = GRadioButtonQuestion
  override type RBDQ = GRadioButtonVectorQuestion
//  override type MemoDB =

  /**
    * Tell the backend to accept the answer associated with this ANSWERED task.
    *
    * @param ts ANSWERED tasks.
    * @return Some ACCEPTED tasks if successful.
    */
  override protected def accept(ts: List[Task]): Option[List[Task]] = ???

  /**
    * Get the budget from the backend.
    *
    * @return Some budget if successful.
    */
  override protected def backend_budget(): Option[BigDecimal] = ???

  /**
    * Cancel the given tasks.
    *
    * @param ts      A list of tasks to cancel.
    * @param toState Which scheduler state tasks should become after cancellation.
    * @return Some list of cancelled tasks if successful.
    */
  override protected def cancel(ts: List[Task], toState: SchedulerState.Value): Option[List[Task]] = ???

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
  override protected def post(ts: List[Task], exclude_worker_ids: List[String]): Option[List[Task]] = ???

  /**
    * Tell the backend to reject the answer associated with this ANSWERED task.
    *
    * @param ts_reasons A list of pairs of ANSWERED tasks and their rejection reasons.
    * @return Some REJECTED tasks if succesful.
    */
  override protected def reject(ts_reasons: List[(Task, String)]): Option[List[Task]] = ???

  /**
    * Ask the backend to retrieve answers given a list of RUNNING tasks. Invariant:
    * the size of the list of input tasks == the size of the list of the output
    * tasks. The virtual_time parameter is ignored when not running in simulator mode.
    *
    * @param ts           A list of RUNNING tasks.
    * @param current_time The current virtual time.
    * @return Some list of RUNNING, RETRIEVED, or TIMEOUT tasks if successful.
    */
  override protected def retrieve(ts: List[Task], current_time: Date): Option[List[Task]] = ???

  def Option(id: Symbol, text: String) = new GQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new GQuestionOption(id, text, image_url)

  protected def CBQFactory()  = new GCheckboxQuestion
  protected def CBDQFactory() = new GCheckboxVectorQuestion
  protected def MEQFactory()  = new GMultiEstimationQuestion
  protected def EQFactory()   = new GEstimationQuestion
  protected def FTQFactory()  = new GFreeTextQuestion
  protected def FTDQFactory() = new GFreeTextVectorQuestion
  protected def RBQFactory()  = new GRadioButtonQuestion
  protected def RBDQFactory() = new GRadioButtonVectorQuestion
  override protected def MemoDBFactory(): MemoDB = ???
}
