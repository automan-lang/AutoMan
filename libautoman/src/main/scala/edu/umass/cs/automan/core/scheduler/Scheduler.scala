package edu.umass.cs.automan.core.scheduler

import java.util.Date

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.mock.MockAnswer
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.policy.validation.ValidationPolicy
import edu.umass.cs.automan.core.util.{Stopwatch, Utilities}
import scala.collection.mutable

/**
 * Controls scheduling of tasks for a given question.
 *
 * Note on virtual ticks:
 * 1. The user supplies mock answers, each with a delay_in_s parameter.
 * 2. When the scheduler starts up, if a question comes with mock answers,
 *    the scheduler knows that it should use virtual ticks.
 * 3. For each "tick group" (the set of answers with the same delay_in_s),
 *    the scheduler advances exactly one loop iteration.
 * 4. When the scheduler calls backend operations, it forwards the current
 *    time, virtual or otherwise.  The backend should only return
 *    answered thunks when the current time is after the time of the
 *    response.
 *
 * @param question
 * @param backend
 */
class Scheduler(val question: Question,
                val backend: AutomanAdapter) {
  // save startup time
  val init_time = new Date()
  val use_virt = question.mock_answers.nonEmpty

  // init policies
  question.init_validation_policy()
  question.init_price_policy()
  question.init_timeout_policy()

  /** Crowdsources a task on the desired backend, scheduling and
    * rescheduling enough jobs until the chosen quality-control
    * mechanism is confident in the result, and paying for answers
    * where appropriate.
    */
  def run() : Question#AA = {
    // run startup hook
    backend.question_startup_hook(question, init_time)

    // Was this computation interrupted? If there's a memoizer instance
    // restore tasks from scheduler trace.
    val tasks: List[Task] = backend.memo_restore(question)

    DebugLog("Found " + tasks.size + " saved Answers in database.", LogLevelInfo(), LogType.SCHEDULER, question.id)

    // set initial conditions and call scheduler loop
    run_loop(tasks)
  }

  private def initTickQueue[A](ans: List[MockAnswer[A]]) : mutable.PriorityQueue[Long] = {
    val timeOrd = new Ordering[Long] {
      def compare(o1: Long, o2: Long) = -o1.compare(o2)
    }
    val q = new mutable.PriorityQueue[Long]()(timeOrd)
    if(ans.nonEmpty) {
      val times = ans.map(_.time_delta_in_ms).distinct
      q ++= times
      Some(q)
    }
    q
  }

  private def realTick() : Long = {
    Utilities.elapsedMilliseconds(init_time, new Date())
  }

  private def run_loop(tasks: List[Task]) : Question#AA = {
    val _virtual_times = initTickQueue(question.mock_answers)  // ms quanta
    var _current_time = // ms since start
      if (_virtual_times.nonEmpty) {
        val ct = // pull from the queue for simulations
          _virtual_times.dequeue()
        DebugLog("Virtual clock starts at " + ct + " ms.", LogLevelInfo(), LogType.SCHEDULER, question.id)
        ct
      } else {
        0L
      }
    val _vp = question.validation_policy_instance

    var _timeout_occurred = false
    var _all_tasks = tasks
    var _done = _vp.is_done(_all_tasks) // check for restored memo tasks

    val answer = try {
      while(!_done) {
        // process timeouts
        val (__tasks, __suffered_timeout) = process_timeouts(_all_tasks, _current_time)
        // get list of workers who may not re-participate
        val __blacklist = _vp.blacklisted_workers(__tasks)
        // filter duplicate work
        val __dedup_tasks = _vp.mark_duplicates(__tasks)
        // post more tasks as needed
        val __new_tasks = post_as_needed(__dedup_tasks, backend, question, __suffered_timeout, __blacklist)
        // update virtual_ticks with new timeouts (in milliseconds)
        if (use_virt) {
          _virtual_times ++= __new_tasks.map(_.timeout_in_s).distinct.map(_.toLong * 1000)
        }

        // Update memo state and yield to let other threads get some work done
        memo_and_yield(__dedup_tasks ::: __new_tasks)

        // ask the backend to retrieve answers for all RUNNING tasks
        val (__running_tasks, __unrunning_tasks) = (__dedup_tasks ::: __new_tasks).partition(_.state == SchedulerState.RUNNING)
        assert(__running_tasks.size > 0)
        DebugLog("Retrieving answers for " + __running_tasks.size + " running tasks from backend.", LogLevelInfo(), LogType.SCHEDULER, question.id)
        val __answered_tasks = backend.retrieve(__running_tasks, Utilities.xMillisecondsFromDate(_current_time, init_time))
        assert(retrieve_invariant(__running_tasks, __answered_tasks))

        // complete list of tasks
        val __all_tasks = __answered_tasks ::: __unrunning_tasks

        // memoize tasks again
        backend.memo_save(question, __all_tasks)

        // continue?
        _done = _vp.is_done(__all_tasks)

        // update state
        _all_tasks = __all_tasks
        _current_time = if (use_virt) {
          val t = if (_virtual_times.nonEmpty) {
            // pull from the queue for simulations
            _virtual_times.dequeue()
          } else {
            _current_time + 1000L
          }
          DebugLog("Advancing virtual clock to " + t + " ms.", LogLevelInfo(), LogType.SCHEDULER, question.id)
          t
        } else {
          realTick()
        }
      }

      // pay for answers
      _all_tasks = accept_reject_and_cancel(_all_tasks, _vp, backend)

      // return answer
      _vp.select_answer(_all_tasks)
    } catch {
      case o: OverBudgetException =>
        _vp.select_over_budget_answer(_all_tasks, o.need, o.have)
    }

    // save one more time
    DebugLog("Saving final state of " + _all_tasks.size + " tasks to database.", LogLevelInfo(), LogType.SCHEDULER, question.id)
    backend.memo_save(question, _all_tasks)

    // run shutdown hook
    backend.question_shutdown_hook(question)

    answer
  }

  def process_timeouts(ts: List[Task], current_tick: Long) : (List[Task],Boolean) = {
    // find all timeouts
    val (timeouts,otherwise) = ts.partition { t =>
      t.state == SchedulerState.RUNNING &&
      t.is_timedout(Utilities.xMillisecondsFromDate(current_tick, init_time))
    }
    if (timeouts.size > 0) {
      DebugLog("Cancelling " + timeouts.size + " timed-out tasks.", LogLevelInfo(), LogType.SCHEDULER, question.id)
    }

    // cancel and make state TIMEOUT
    val timed_out = timeouts.map(backend.cancel).map(_.copy_as_timeout())
    // return all updated Task objects and signal whether timeout occurred
    (timed_out ::: otherwise, timeouts.nonEmpty)
  }

  def memo_and_yield(ts: List[Task]) : Unit = {
    backend.memo_save(question, ts)
    Thread.`yield`()
  }

  /**
   * Post new tasks if needed. Returns only newly-created tasks.
   * @param tasks The complete list of tasks.
   * @param question Question data.
   * @param suffered_timeout True if any tasks suffered a timeout on the last iteration.
   * @return A list of newly-created tasks.
   */
  def post_as_needed(tasks: List[Task],
                     backend: AutomanAdapter,
                     question: Question,
                     suffered_timeout: Boolean,
                     blacklist: List[String]) : List[Task] = {
    val s = question.validation_policy_instance

    // are any tasks still running?
    if (tasks.count(_.state == SchedulerState.RUNNING) == 0) {
      // no, so post more
      // compute set of new tasks
      val new_tasks = s.spawn(tasks, suffered_timeout)
      assert(spawn_invariant(new_tasks))
      // can we afford these?
      val cost = total_cost(tasks ::: new_tasks)
      if (question.budget < cost) {
        // no
        DebugLog("Over budget. Need: " + cost.toString() + ", have: " + question.budget.toString(), LogLevelWarn(), LogType.SCHEDULER, question.id)
        throw new OverBudgetException(cost, question.budget)
      } else {
        // yes, so post and return all posted tasks
        val posted = backend.post(new_tasks, blacklist)
        DebugLog("Posting " + posted.size + " tasks to backend.", LogLevelInfo(), LogType.SCHEDULER, question.id)
        posted
      }
    } else {
      // do nothing for now
      List.empty
    }
  }

  /**
   * Accepts and rejects tasks on the backend.  Returns all tasks.
   * @param all_tasks All tasks.
   * @param strategy The ValidationStrategy.
   * @param backend A reference to the backend AutomanAdapter.
   * @return The tasks passed in, with new states.
   */
  def accept_reject_and_cancel[A](all_tasks: List[Task],
                                  strategy: ValidationPolicy,
                                  backend: AutomanAdapter) : List[Task] = {
    val to_cancel = strategy.tasks_to_cancel(all_tasks)
    val to_accept = strategy.tasks_to_accept(all_tasks)
    val to_reject = strategy.tasks_to_reject(all_tasks)

    assert(to_accept.forall(_.state == SchedulerState.ANSWERED), all_tasks.map(_.state).mkString(", "))
    assert(to_reject.forall(_.state == SchedulerState.ANSWERED), all_tasks.map(_.state).mkString(", "))

    val correct_answer = strategy.rejection_response(to_accept)

    val action_items = to_cancel ::: to_accept ::: to_reject

    val remaining_tasks = all_tasks.filterNot(action_items.contains(_))

    val cancelled = to_cancel.map(backend.cancel)
    assert(all_set_invariant(to_cancel, cancelled, SchedulerState.CANCELLED))
    val accepted = to_accept.map(backend.accept)
    assert(all_set_invariant(to_accept, accepted, SchedulerState.ACCEPTED))
    val rejected = to_reject.map(backend.reject(_, correct_answer))
    assert(all_set_invariant(to_reject, rejected, SchedulerState.REJECTED))
    remaining_tasks ::: cancelled ::: accepted ::: rejected
  }

  /**
   * Calculates the total cost of all tasks that might
   * potentially be accepted.
   * @param tasks The complete list of tasks.
   * @return The amount spent.
   */
  def total_cost[A](tasks: List[Task]) : BigDecimal = {
    tasks.filter { t =>
      t.state != SchedulerState.CANCELLED &&
      t.state != SchedulerState.DUPLICATE &&
      t.state != SchedulerState.TIMEOUT &&
      t.state != SchedulerState.REJECTED &&
      !t.from_memo
    }.foldLeft(BigDecimal(0)) { case (acc,t) => acc + t.cost }
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