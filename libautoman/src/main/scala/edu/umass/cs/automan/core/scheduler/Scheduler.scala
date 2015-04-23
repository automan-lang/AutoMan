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
    // restore thunks from scheduler trace.
    val thunks: List[Thunk] = memo.restore(question)

    DebugLog("Found " + thunks.size + " saved Answers in database.", LogLevel.INFO, LogType.SCHEDULER, question.id)

    // set initial conditions and call scheduler loop
    run_loop(thunks, suffered_timeout = false)
  }

  private def run_loop(thunks: List[Thunk], suffered_timeout: Boolean) : Question#AA = {
    val s = question.strategy_instance

    var _timeout_occurred = false
    var _all_thunks = thunks

    while(!s.is_done(_all_thunks)) {
      val __thunks = _all_thunks
      // get list of workers who may not re-participate
      val blacklist = s.blacklisted_workers(__thunks)
      // filter duplicate work
      val dedup_thunks = s.mark_duplicates(__thunks)
      // post more tasks as needed

      val new_thunks = try {
        post_as_needed(dedup_thunks, backend, question, suffered_timeout, blacklist)
      } catch {
        case o: OverBudgetException[_] => return s.select_over_budget_answer(thunks)
      }
      // The scheduler waits here to give the crowd time to answer.
      // Sleeping also informs the scheduler that this thread may yield
      // its CPU time.  While we wait, we also update memo state.
      memo_and_sleep(poll_interval_in_s * 1000, dedup_thunks ::: new_thunks, memo)

      // ask the backend to retrieve answers for all RUNNING thunks
      val (running_thunks, dead_thunks) = (dedup_thunks ::: new_thunks).partition(_.state == SchedulerState.RUNNING)
      assert(running_thunks.size > 0)
      DebugLog("Retrieving answers for " + running_thunks.size + " running tasks from backend.", LogLevel.INFO, LogType.SCHEDULER, question.id)
      val answered_thunks = backend.retrieve(running_thunks)
      assert(retrieve_invariant(running_thunks, answered_thunks))

      // complete list of thunks
      val all_thunks = answered_thunks ::: dead_thunks

      // memoize thunks again
      memo.save(question, all_thunks)

      synchronized {
        _all_thunks = all_thunks
        _timeout_occurred = timeout_occurred(answered_thunks)
      }
    }

    // select answer
    val answer = s.select_answer(_all_thunks)

    // pay for answers
    val _final_thunks = accept_and_reject(
                          s.thunks_to_accept(_all_thunks),
                          s.thunks_to_reject(_all_thunks),
                          answer.value.toString,
                          backend
                        )

    // save one more time
    DebugLog("Saving state of " + _final_thunks.size + " thunks to database.", LogLevel.INFO, LogType.SCHEDULER, question.id)
    memo.save(question, _final_thunks)

    // run shutdown hook
    backend.question_shutdown_hook(question)

    answer
  }

  def memo_and_sleep(wait_time_ms: Int, ts: List[Thunk], memo: Memo) : Unit = {
    val t = Stopwatch {
      DebugLog("Saving state of " + ts.size + " thunks to database.", LogLevel.INFO, LogType.SCHEDULER, question.id)
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

  def cost_for_thunks(thunks: List[Thunk]) : BigDecimal = {
    thunks.foldLeft(BigDecimal(0)) { case (acc, t) => acc + t.cost }
  }

  /**
   * Check to see whether a timeout occurred given a list of Thunks.
   * @param thunks A list of Thunks.
   * @return True if at least one timeout occurred.
   */
  def timeout_occurred(thunks: List[Thunk]) : Boolean = {
    thunks.count(_.state == SchedulerState.TIMEOUT) > 0
  }

  /**
   * Post new tasks if needed. Returns only newly-created thunks.
   * @param thunks The complete list of thunks.
   * @param question Question data.
   * @param suffered_timeout True if any thunks suffered a timeout on the last iteration.
   * @return A list of newly-created Thunks.
   */
  def post_as_needed(thunks: List[Thunk],
                     backend: AutomanAdapter,
                     question: Question,
                     suffered_timeout: Boolean,
                     blacklist: List[String]) : List[Thunk] = {
    val s = question.strategy_instance

    // are any thunks still running?
    if (thunks.count(_.state == SchedulerState.RUNNING) == 0) {
      // no, so post more
      val new_thunks = s.spawn(thunks, suffered_timeout)
      assert(spawn_invariant(new_thunks))
      // can we afford these?
      val cost = cost_for_thunks(thunks ::: new_thunks)
      if (question.budget < cost) {
        val answer = s.select_answer(thunks)
        DebugLog("Over budget. Need: " + cost.toString() + ", have: " + question.budget.toString(), LogLevel.WARN, LogType.SCHEDULER, question.id)
        throw new OverBudgetException[Question#A](answer.value, answer.cost)
      } else {
        // yes, so post and return all posted thunks
        val posted = backend.post(new_thunks, blacklist)
        DebugLog("Posting " + posted.size + " tasks to backend.", LogLevel.INFO, LogType.SCHEDULER, question.id)
        posted
      }
    } else {
      List.empty
    }
  }

  /**
   * Accepts and rejects tasks on the backend.  Returns all Thunks.
   * @param to_accept A list of Thunks to be accepted.
   * @param to_reject A list of Thunks to be rejected.
   * @param correct_answer A stringified version of the correct answer.
   * @param backend A reference to the backend AutomanAdapter.
   * @return The amount of money spent.
   */
  def accept_and_reject[A](to_accept: List[Thunk], to_reject: List[Thunk], correct_answer: String, backend: AutomanAdapter) : List[Thunk] = {
    val accepted = to_accept.map(backend.accept)
    assert(all_set_invariant(to_accept, accepted, SchedulerState.ACCEPTED))
    val rejected = to_reject.map(backend.reject(_, correct_answer))
    assert(all_set_invariant(to_reject, rejected, SchedulerState.REJECTED))
    accepted ::: rejected
  }

  /**
   * Calculates the total cost of all ACCEPTED thunks.
   * @param thunks The complete list of Thunks.
   * @return The amount spent.
   */
  def total_cost[A](thunks: List[Thunk]) : BigDecimal = {
    thunks.filter(_.state == SchedulerState.ACCEPTED).foldLeft(BigDecimal(0)) { case (acc,t) => acc + t.cost }
  }

  // INVARIANTS

  /**
   * Given a list of RUNNING thunks and a list of thunks returned from
   * the AutomanAdapter.retrieve method, ensure that a number of
   * invariants hold.
   * @param running A list of RUNNING thunks.
   * @param answered A list of thunks returned by the AutomanAdapter.retrieve method.
   * @return True if all invariants hold.
   */
  def retrieve_invariant[A](running: List[Thunk], answered: List[Thunk]) : Boolean = {
    // all of the running thunks should actually be RUNNING
    running.count(_.state == SchedulerState.RUNNING) == running.size &&
      // the number of thunks given should be the same number returned
      answered.size == running.size &&
      // returned thunks should all either be RUNNING, RETRIEVED, DUPLICATE, or TIMEOUT
      answered.count { t =>
        t.state == SchedulerState.RUNNING ||
          t.state == SchedulerState.ANSWERED ||
          t.state == SchedulerState.DUPLICATE ||
          t.state == SchedulerState.TIMEOUT
      } == running.size
  }

  /**
   * The list of newly-spawned thunks should never be zero.
   * @param new_thunks A list of newly-spawned thunks.
   * @return True if the invariant holds.
   */
  def spawn_invariant[A](new_thunks: List[Thunk]) : Boolean = {
    new_thunks.size != 0
  }

  /**
   * Returns true if all of the Thunks from the before list are set to the
   * given state in the after list.
   * @param before A list of Thunks.
   * @param after A list of Thunks.
   * @param state The state to check.
   * @return True if the invariant holds.
   */
  def all_set_invariant[A](before: List[Thunk], after: List[Thunk], state: SchedulerState.Value) : Boolean = {
    val after_set = after.map { t => t.thunk_id }.toSet
    before.foldLeft(true){ case (acc,t) => acc && after_set.contains(t.thunk_id) }
  }
}