package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

abstract class AggregationPolicy(question: Question) {
  class PrematureAggregationException(methodname: String, classname: String)
    extends Exception(methodname + " called prematurely in " + classname)

  /**
   * Returns a list of blacklisted worker_ids given a
   * set of tasks, completed or not.
   * @param tasks The complete list of tasks.
   * @return A list of worker IDs.
   */
  def blacklisted_workers(tasks: List[Task]): List[String] = {
    tasks.flatMap(_.worker_id).distinct
  }

  /**
   * Given a list of tasks, this method returns the same list with
   * all but one task marked as DUPLICATE for each subset submitted by each
   * distinct worker.  The task left as ANSWERED is chosen arbitrarily (the
   * first one encountered).
   * @param tasks A list of ANSWERED tasks.
   * @return A list of ANSWERED and DUPLICATE tasks.
   */
  def mark_duplicates(tasks: List[Task]): List[Task] = {
    val (answered_tasks, unanswered_tasks) = tasks.partition(_.state == SchedulerState.ANSWERED)

    answered_tasks.groupBy(_.worker_id).flatMap { case (worker_id, ts) =>
      ts.head :: ts.tail.map(_.copy_as_duplicate())
    }.toList

    answered_tasks ::: unanswered_tasks
  }

  /**
   * Returns true if the strategy has enough data to stop scheduling work.
   * @param tasks The complete list of scheduled tasks.
   * @return
   */
  def is_done(tasks: List[Task]) : Boolean

  /**
   * Returns a string explaining why the worker's answer was not accepted.
   * @param tasks The list of accepted tasks. Used to determine the correct answer.
   * @return Explanation string.
   */
  def rejection_response(tasks: List[Task]) : String

  /**
   * Returns the top answer.
   * @param tasks The complete list of tasks.
   * @return Top answer
   */
  def select_answer(tasks: List[Task]) : Question#AA

  /**
   * Returns an appropriate response for when the computation ran out of money.
   * @param tasks The complete list of tasks.
   * @param need The smallest amount of money needed to complete the computation under optimistic assumptions.
   * @param have The amount of money we have.
   * @return A low-confidence or over-budget answer.
   */
  def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal) : Question#AA

  /**
   * Computes the number of tasks needed to satisfy the quality-control
   * algorithm given the already-collected list of tasks. Returns only
   * newly-created tasks.
   *
   * @param tasks The complete list of previously-scheduled tasks
   * @param suffered_timeout True if any of the latest batch of tasks suffered a timeout.
   * @return A list of new tasks to schedule on the backend.
   */
  def spawn(tasks: List[Task], suffered_timeout: Boolean): List[Task]

  def tasks_to_accept(tasks: List[Task]): List[Task]

  def tasks_to_cancel(tasks: List[Task]): List[Task] = {
    tasks.filter { t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING
    }.filter(_.state != SchedulerState.CANCELLED )
  }
  def tasks_to_reject(tasks: List[Task]): List[Task]

  protected def unique_by_date(ts: List[Task]) = {
    // worker_id should always be set for RETRIEVED and PROCESSED
    val tw_groups = ts.groupBy( t => t.worker_id.get )
    // sort by creation date and take the first
    tw_groups.map{ case(worker_id,tz) =>
      tz.sortBy{ t => t.created_at }.head
    }.toList
  }

  protected def completed_tasks(tasks: List[Task]) = {
    // tasks should be
    tasks.filter(t =>
      t.state == SchedulerState.ANSWERED ||   // retrieved from MTurk
      t.state == SchedulerState.REJECTED ||   // OR rejected
      t.state == SchedulerState.ACCEPTED      // OR accepted
    )
  }

  // tasks that have either been retrieved from memo
  // or pulled from backend; and no more than one per worker
  protected def completed_workerunique_tasks(tasks: List[Task]) = {
    // completed
    val completed = completed_tasks(tasks)

    // check that list is empty
    if (completed.isEmpty) {
      List()
    } else {
      // if a worker completed more than one, take the first
      unique_by_date(completed)
    }
  }

  protected def outstanding_tasks(tasks: List[Task]) = {
    // basically, not TIMEOUTs and REJECTs
    val outstanding = tasks.filter(t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING
    )
    outstanding
  }
}