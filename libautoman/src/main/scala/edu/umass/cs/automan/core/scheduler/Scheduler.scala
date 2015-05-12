package edu.umass.cs.automan.core.scheduler

import java.util.Date
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{ScalarOutcome, Outcome, AbstractAnswer}
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.util.Stopwatch

import scala.concurrent._

class Scheduler(val question: Question,
                   val backend: AutomanAdapter,
                   val memo: Memo,
                   val poll_interval_in_s: Int,
                   val time_opt: Option[Date]) {
  def this(question: Question,
           adapter: AutomanAdapter,
           memo: Memo,
           poll_interval_in_s: Int) = this(question, adapter, memo, poll_interval_in_s, None)

  /** Crowdsources a task on the desired backend, scheduling and
    * rescheduling enough jobs until the chosen quality-control
    * mechanism is confident in the result, and paying for answers
    * where appropriate.
    */
  def run() : Question#AA = {
    // run startup hook
    backend.question_startup_hook(question)

    // Was this computation interrupted? If there's a memoizer instance
    // restore tasks from scheduler trace.
    val tasks: List[Task] = memo.restore(question)

    DebugLog("Found " + tasks.size + " saved Answers in database.", LogLevel.INFO, LogType.SCHEDULER, question.id)

    // set initial conditions and call scheduler loop
    run_loop(tasks, suffered_timeout = false)
  }

  private def run_loop(tasks: List[Task], suffered_timeout: Boolean) : Question#AA = {
    val s = question.strategy_instance

    var _timeout_occurred = false
    var _all_tasks = tasks
    var _round = 1
    var _done = false

    val answer = try {
      while(!_done) {
        val __tasks = _all_tasks
        // get list of workers who may not re-participate
        val blacklist = s.blacklisted_workers(__tasks)
        // filter duplicate work
        val dedup_tasks = s.mark_duplicates(__tasks)
        // post more tasks as needed
        val new_tasks = post_as_needed(dedup_tasks, _round, backend, question, suffered_timeout, blacklist)

        // The scheduler waits here to give the crowd time to answer.
        // Sleeping also informs the scheduler that this thread may yield
        // its CPU time.  While we wait, we also update memo state.
        memo_and_sleep(poll_interval_in_s * 1000, dedup_tasks ::: new_tasks, memo)

        // ask the backend to retrieve answers for all RUNNING tasks
        val (running_tasks, dead_tasks) = (dedup_tasks ::: new_tasks).partition(_.state == SchedulerState.RUNNING)
        assert(running_tasks.size > 0)
        DebugLog("Retrieving answers for " + running_tasks.size + " running tasks from backend.", LogLevel.INFO, LogType.SCHEDULER, question.id)
        val answered_tasks = backend.retrieve(running_tasks)
        assert(retrieve_invariant(running_tasks, answered_tasks))

        // complete list of tasks
        val all_tasks = answered_tasks ::: dead_tasks

        // memoize tasks again
        memo.save(question, all_tasks)

        // continue?
        _done = s.is_done(all_tasks, _round)

        synchronized {
          _all_tasks = all_tasks
          _timeout_occurred = timeout_occurred(answered_tasks)
          _round += 1
        }
      }

      // pay for answers
      val cancelled_tasks = s.tasks_to_cancel(_all_tasks)
      val accepted_tasks = s.tasks_to_accept(_all_tasks)
      val rejected_tasks = s.tasks_to_reject(_all_tasks)
      accept_reject_and_cancel(
        accepted_tasks,
        rejected_tasks,
        cancelled_tasks,
        s.rejection_response(accepted_tasks),
        backend
      )

      s.select_answer(_all_tasks)

    } catch {
      case o: OverBudgetException =>
        s.select_over_budget_answer(_all_tasks, o.need, o.have)
    }

    // save one more time
    DebugLog("Saving final state of " + _all_tasks.size + " tasks to database.", LogLevel.INFO, LogType.SCHEDULER, question.id)
    memo.save(question, _all_tasks)

    // run shutdown hook
    backend.question_shutdown_hook(question)

    answer
  }

  def memo_and_sleep(wait_time_ms: Int, ts: List[Task], memo: Memo) : Unit = {
    val t = Stopwatch {
      DebugLog("Saving state of " + ts.size + " tasks to database.", LogLevel.INFO, LogType.SCHEDULER, question.id)
      memo.save(question, ts)
    }
    val rem_ms = wait_time_ms - t.duration_ms
    if (rem_ms > 0) {
      // wait the remaining amount of time
      DebugLog("Sleeping " + (rem_ms / 1000) + " seconds.", LogLevel.INFO, LogType.SCHEDULER, question.id)
      Thread.sleep(rem_ms)
    } else {
      // even if we don't need to wait, we should give the JVM
      // the opportunity to schedule another thread
      Thread.`yield`()
    }
  }

  def cost_for_tasks(tasks: List[Task]) : BigDecimal = {
    tasks.foldLeft(BigDecimal(0)) { case (acc, t) => acc + t.cost }
  }

  /**
   * Check to see whether a timeout occurred given a list of tasks.
   * @param tasks A list of tasks.
   * @return True if at least one timeout occurred.
   */
  def timeout_occurred(tasks: List[Task]) : Boolean = {
    tasks.count(_.state == SchedulerState.TIMEOUT) > 0
  }

  /**
   * Post new tasks if needed. Returns only newly-created tasks.
   * @param tasks The complete list of tasks.
   * @param round How many QC rounds performed so far
   * @param question Question data.
   * @param suffered_timeout True if any tasks suffered a timeout on the last iteration.
   * @return A list of newly-created tasks.
   */
  def post_as_needed(tasks: List[Task],
                     round: Int,
                     backend: AutomanAdapter,
                     question: Question,
                     suffered_timeout: Boolean,
                     blacklist: List[String]) : List[Task] = {
    val s = question.strategy_instance

    // are any tasks still running?
    if (tasks.count(_.state == SchedulerState.RUNNING) == 0) {
      // no, so post more
      val new_tasks = s.spawn(tasks, round, suffered_timeout)
      assert(spawn_invariant(new_tasks))
      // can we afford these?
      val cost = cost_for_tasks(tasks ::: new_tasks)
      if (question.budget < cost) {
        DebugLog("Over budget. Need: " + cost.toString() + ", have: " + question.budget.toString(), LogLevel.WARN, LogType.SCHEDULER, question.id)
        throw new OverBudgetException(cost, question.budget)
      } else {
        // yes, so post and return all posted tasks
        val posted = backend.post(new_tasks, blacklist)
        DebugLog("Posting " + posted.size + " tasks to backend.", LogLevel.INFO, LogType.SCHEDULER, question.id)
        posted
      }
    } else {
      List.empty
    }
  }

  /**
   * Accepts and rejects tasks on the backend.  Returns all tasks.
   * @param to_cancel A list of tasks to be canceled.
   * @param to_accept A list of tasks to be accepted.
   * @param to_reject A list of tasks to be rejected.
   * @param correct_answer A stringified version of the correct answer.
   * @param backend A reference to the backend AutomanAdapter.
   * @return The amount of money spent.
   */
  def accept_reject_and_cancel[A](to_accept: List[Task],
                                  to_reject: List[Task],
                                  to_cancel: List[Task],
                                  correct_answer: String,
                                  backend: AutomanAdapter) : List[Task] = {
    val canceled = to_cancel.map(backend.cancel)
    assert(all_set_invariant(to_cancel, canceled, SchedulerState.CANCELLED))
    val accepted = to_accept.map(backend.accept)
    assert(all_set_invariant(to_accept, accepted, SchedulerState.ACCEPTED))
    val rejected = to_reject.map(backend.reject(_, correct_answer))
    assert(all_set_invariant(to_reject, rejected, SchedulerState.REJECTED))
    accepted ::: rejected
  }

  /**
   * Calculates the total cost of all ACCEPTED tasks.
   * @param tasks The complete list of tasks.
   * @return The amount spent.
   */
  def total_cost[A](tasks: List[Task]) : BigDecimal = {
    tasks.filter(_.state == SchedulerState.ACCEPTED).foldLeft(BigDecimal(0)) { case (acc,t) => acc + t.cost }
  }

  // INVARIANTS

  /**
   * Given a list of RUNNING tasks and a list of tasks returned from
   * the AutomanAdapter.retrieve method, ensure that a number of
   * invariants hold.
   * @param running A list of RUNNING tasks.
   * @param answered A list of tasks returned by the AutomanAdapter.retrieve method.
   * @return True if all invariants hold.
   */
  def retrieve_invariant[A](running: List[Task], answered: List[Task]) : Boolean = {
    // all of the running tasks should actually be RUNNING
    running.count(_.state == SchedulerState.RUNNING) == running.size &&
      // the number of tasks given should be the same number returned
      answered.size == running.size &&
      // returned tasks should all either be RUNNING, RETRIEVED, DUPLICATE, or TIMEOUT
      answered.count { t =>
        t.state == SchedulerState.RUNNING ||
          t.state == SchedulerState.ANSWERED ||
          t.state == SchedulerState.DUPLICATE ||
          t.state == SchedulerState.TIMEOUT
      } == running.size
  }

  /**
   * The list of newly-spawned tasks should never be zero.
   * @param new_tasks A list of newly-spawned tasks.
   * @return True if the invariant holds.
   */
  def spawn_invariant[A](new_tasks: List[Task]) : Boolean = {
    new_tasks.size != 0
  }

  /**
   * Returns true if all of the tasks from the before list are set to the
   * given state in the after list.
   * @param before A list of tasks.
   * @param after A list of tasks.
   * @param state The state to check.
   * @return True if the invariant holds.
   */
  def all_set_invariant[A](before: List[Task], after: List[Task], state: SchedulerState.Value) : Boolean = {
    val after_set = after.map { t => t.task_id }.toSet
    before.foldLeft(true){ case (acc,t) => acc && after_set.contains(t.task_id) }
  }
}