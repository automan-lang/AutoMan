package org.automanlang.core.policy.aggregation

import java.util.UUID

import org.automanlang.core.logging.{LogType, LogLevelInfo, DebugLog}
import org.automanlang.core.policy._
import org.automanlang.core.question.{Response, Question}
import org.automanlang.core.scheduler.{SchedulerState, Task}

abstract class AggregationPolicy(question: Question) {
  class PrematureAggregationException(methodname: String, classname: String)
    extends Exception(methodname + " called prematurely in " + classname)

  /**
    * Determines whether a policy allows for canceling running tasks.
    * If true, is_done will be called more often, increasing the
    * required confidence level to terminate.  Note that an early
    * termination check is always conducted when timeouts occur
    * regardless of this setting.
    */
  def allow_early_termination() : Boolean = false

  /**
   * Returns a list of banned worker_ids given a
   * set of tasks, completed or not.
   * @param tasks The complete list of tasks.
   * @return A list of worker IDs.
   */
  def banned_workers(tasks: List[Task]): List[String] = {
    tasks.flatMap(_.worker_id).distinct
  }

  /**
    * Returns true if the strategy has enough data to stop scheduling work.
    * @param tasks The complete list of scheduled tasks.
    * @param num_comparisons The number of times this function has been called, inclusive.
    * @return (true iff done, new num_comparisons)
    */
  def is_done(tasks: List[Task], num_comparisons: Int) : (Boolean,Int)

  def not_final(task: Task) : Boolean = {
    task.state != SchedulerState.ACCEPTED &&
      task.state != SchedulerState.REJECTED &&
      task.state != SchedulerState.CANCELLED &&
      task.state != SchedulerState.TIMEOUT
  }

  protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal) : Int

  /**
    * Partitions a set of tasks into those that should be marked as
    * duplicate and those that should not be.  All tasks passed in
    * are passed back out.
    * @param tasks A list of tasks
    * @return (list of non-duplicate tasks, list of duplicate tasks)
    */
  def partition_duplicates(tasks: List[Task]): (List[Task],List[Task]) = {
    // unanswered tasks cannot be duplicates
    val (answered_tasks, unanswered_tasks) = tasks.partition(_.state == SchedulerState.ANSWERED)

    // group by worker, and then partition by duplicate status
    val (not_dupes: List[Task],dupes: List[Task]) = answered_tasks.groupBy(_.worker_id).map { case (_,ts) =>
      val not_a_dupe = ts.head
      val bunch_of_dupes = ts.tail
      (not_a_dupe, bunch_of_dupes)
    }.foldLeft((List[Task](),List[Task]())) { case ((nonduplicates,duplicates),(not_a_dupe,bunch_of_dupes)) =>
      (not_a_dupe :: nonduplicates, bunch_of_dupes ::: duplicates)
    }

    (unanswered_tasks ::: not_dupes, dupes)
  }

  /**
   * Returns a string explaining why the worker's answer was not accepted.
   * @param tasks The list of accepted tasks. Used to determine the correct answer.
   * @return Explanation string.
   */
  def rejection_response(tasks: List[Task]) : String

  /**
   * Returns the top answer.
   * @param tasks The complete list of tasks.
   * @param num_comparisons The number of times is_done has been called.
   * @return Top answer
   */
  def select_answer(tasks: List[Task], num_comparisons: Int) : Question#AA

  /**
   * Returns an appropriate response for when the computation ran out of money.
   * @param tasks The complete list of tasks.
   * @param need The smallest amount of money needed to complete the computation under optimistic assumptions.
   * @param have The amount of money we have.
   * @param num_comparisons The number of times is_done has been called.
   * @return A low-confidence or over-budget answer.
   */
  def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal, num_comparisons: Int) : Question#AA

  /**
   * Computes the number of tasks needed to satisfy the quality-control
   * algorithm given the already-collected list of tasks. Returns only
   * newly-created tasks.
   *
   * @param tasks The complete list of previously-scheduled tasks
   * @param suffered_timeout True if any of the latest batch of tasks suffered a timeout.
   * @param num_comparisons How many times we've performed a power analysis, INCLUDING THIS TIME.
   * @return A list of new tasks to schedule on the backend.
   */
  def spawn(tasks: List[Task], suffered_timeout: Boolean, num_comparisons: Int): List[Task] = {
    // determine current round (starts at zero)
    val cRound = currentRound(tasks)

    // determine timeouts
    val worker_timeout_in_s = question._timeout_policy_instance.calculateWorkerTimeout(tasks, cRound, suffered_timeout)
    val task_timeout_in_s = question._timeout_policy_instance.calculateTaskTimeout(worker_timeout_in_s)

    // determine reward
    val reward = question._price_policy_instance.calculateReward(tasks, cRound, suffered_timeout)

    // determine number to spawn
    val num_to_spawn = if (tasks.count(_.state == SchedulerState.RUNNING) == 0) {
      // there are no running tasks
      val min_to_spawn = num_to_run(tasks, num_comparisons, reward)
      // this is an ugly hack for MTurk;
      // TODO: think of better way to deal with MTurk's HIT extension policy
      Math.max(question._minimum_spawn_policy.min, min_to_spawn)
    } else {
      return List[Task]() // Be patient!
    }

    // allocate Task objects
    val now = new java.util.Date()
    val nRound = nextRound(tasks, suffered_timeout)

    DebugLog("Round = " + nRound + " . You should spawn " + num_to_spawn +
      " more Tasks at $" + reward + "/task, " +
      task_timeout_in_s + "s until question timeout, " +
      worker_timeout_in_s + "s until worker task timeout.", LogLevelInfo(), LogType.STRATEGY,
      question.id)

    val new_tasks = (0 until num_to_spawn).map { i =>
      val t = new Task(
        UUID.randomUUID(),
        question,
        nRound,
        task_timeout_in_s,
        worker_timeout_in_s,
        reward,
        now,
        SchedulerState.READY,
        from_memo = false,
        None,
        None,
        now
      )
      t
    }.toList

    new_tasks
  }

  def tasks_to_accept(tasks: List[Task]): List[Task]

  def tasks_to_accept_on_failure(tasks: List[Task]) : List[Task] = {
    val cancels = tasks_to_cancel(tasks).toSet
    completed_tasks(tasks)
      .filter { t =>
        not_final(t) &&
        !cancels.contains(t)
      }
  }

  def tasks_to_cancel(tasks: List[Task]): List[Task] = {
    tasks.filter { t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING
    }
  }
  def tasks_to_reject(tasks: List[Task]): List[Task]

  protected def unique_by_date(ts: List[Task]) = {
    // worker_id should always be set for ANSWERED, ACCEPTED, and REJECTED
    val tw_groups = ts.groupBy( t => t.worker_id.get )
    // sort by creation date and take the first
    tw_groups.map{ case(worker_id,tz) =>
      tz.sortBy{ t => t.created_at }.head
    }.toList
  }

  def completed_tasks(tasks: List[Task]) = {
    // tasks should be
    tasks.filter(t =>
      t.state == SchedulerState.ANSWERED ||   // retrieved from MTurk
      t.state == SchedulerState.REJECTED ||   // OR rejected
      t.state == SchedulerState.ACCEPTED      // OR accepted
    )
  }

  // tasks that have either been retrieved from memo
  // or pulled from backend; and no more than one per worker
  def completed_workerunique_tasks(tasks: List[Task]) = {
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

  // takes tasks, creates array of Responses with optional answer and worker ID
  def getDistribution(tasks: List[Task]) : Array[Response[Question#A]] = {
    // distribution
    tasks.flatMap { t =>
      (t.answer,t.worker_id) match {
        case (Some(ans),Some(worker)) => Some(Response(ans,worker))
        case _ => None
      }
    }.toArray
  }

  // Tasks that are waiting for answers
  def outstanding_tasks(tasks: List[Task]): List[Task] = {
    val outstanding = tasks.filter(t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING
    )
    outstanding
  }

  // Tasks that are answered. Used to determine how many more tasks need to be
  // spawned for new rounds.
  def answered_tasks(tasks: List[Task]): List[Task] = {
    tasks.filter(t =>
      t.state == SchedulerState.ANSWERED ||
      t.state == SchedulerState.ACCEPTED
    )
  }
}
