package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.question.Question

abstract class ValidationStrategy(question: Question) {
  class PrematureValidationCompletionException(methodname: String, classname: String)
    extends Exception(methodname + " called prematurely in " + classname)

  /**
   * Returns a list of blacklisted worker_ids given a
   * set of thunks, completed or not.
   * @param thunks The complete list of thunks.
   * @return A list of worker IDs.
   */
  def blacklisted_workers(thunks: List[Thunk]): List[String] = {
    thunks.flatMap(_.worker_id).distinct
  }

  /**
   * Given a list of thunks, this method returns the same list with
   * all but one thunk marked as DUPLICATE for each subset submitted by each
   * distinct worker.  The thunk left as ANSWERED is chosen arbitrarily (the
   * first one encountered).
   * @param thunks A list of ANSWERED thunks.
   * @return A list of ANSWERED and DUPLICATE thunks.
   */
  def mark_duplicates(thunks: List[Thunk]): List[Thunk] = {
    val (answered_thunks, unanswered_thunks) = thunks.partition(_.state == SchedulerState.ANSWERED)

    answered_thunks.groupBy(_.worker_id).map { case (worker_id, ts) =>
        ts.head :: ts.tail.map(_.copy_as_duplicate())
    }.flatten.toList

    answered_thunks ::: unanswered_thunks
  }

  /**
   * Returns true if the strategy has enough data to stop scheduling work.
   * @param thunks The complete list of scheduled thunks.
   * @return
   */
  def is_done(thunks: List[Thunk]) : Boolean

  /**
   * Returns a string explaining why the worker's answer was not accepted.
   * @param thunks The list of accepted tasks. Used to determine the correct answer.
   * @return Explanation string.
   */
  def rejection_response(thunks: List[Thunk]) : String
  def select_answer(thunks: List[Thunk]) : Question#AA
  def select_over_budget_answer(thunks: List[Thunk], need: BigDecimal, have: BigDecimal) : Question#AA
  /**
   * Computes the number of Thunks needed to satisfy the quality-control
   * algorithm given the already-collected list of Thunks. Returns only
   * newly-created thunks.
   *
   * @param thunks The complete list of previously-scheduled Thunks
   * @param suffered_timeout True if any of the latest batch of Thunks suffered a timeout.
   * @return A list of new Thunks to schedule on the backend.
   */
  def spawn(thunks: List[Thunk], suffered_timeout: Boolean): List[Thunk]
  def thunks_to_accept(thunks: List[Thunk]): List[Thunk]
  def thunks_to_cancel(thunks: List[Thunk]): List[Thunk] = {
    thunks.filter { t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING
    }
  }
  def thunks_to_reject(thunks: List[Thunk]): List[Thunk]

  protected def unique_by_date(ts: List[Thunk]) = {
    // worker_id should always be set for RETRIEVED and PROCESSED
    val tw_groups = ts.groupBy( t => t.worker_id.get )
    // sort by creation date and take the first
    tw_groups.map{ case(worker_id,tz) =>
      tz.sortBy{ t => t.created_at }.head
    }.toList
  }

  protected def completed_thunks(thunks: List[Thunk]) = {
    // thunks should be
    thunks.filter(t =>
      t.state == SchedulerState.ANSWERED ||   // retrieved from MTurk
      t.state == SchedulerState.PROCESSED ||  // OR recalled from a memo DB
      t.state == SchedulerState.ACCEPTED      // OR accepted
    )
  }

  // thunks that have either been retrieved from memo
  // or pulled from backend; and no more than one per worker
  protected def completed_workerunique_thunks(thunks: List[Thunk]) = {
    // if a worker completed more than one, take the first
    unique_by_date(completed_thunks(thunks))
  }

  protected def outstanding_thunks(thunks: List[Thunk]) = {
    // basically, not TIMEOUTs and REJECTs
    val outstanding = thunks.filter(t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING
    )
    // don't count duplicates
    val completed = completed_workerunique_thunks(thunks)
    outstanding ::: completed
  }
}