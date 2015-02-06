package edu.umass.cs.automan.core.scheduler

import java.util.Date
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.question.Question
import scala.annotation.tailrec

class Scheduler[A](val question: Question[A],
                   val backend: AutomanAdapter,
                   val memo_opt: Option[Memo],
                   val poll_interval_in_s: Int,
                   val time_opt: Option[Date]) {
  def this(question: Question[A],
           adapter: AutomanAdapter,
           memoizer: Option[Memo],
           poll_interval_in_s: Int) = this(question, adapter, memoizer, poll_interval_in_s, None)

  /** Crowdsources a task on the desired backend, scheduling and
    * rescheduling enough jobs until the chosen quality-control
    * mechanism is confident in the result, and paying for answers
    * where appropriate.
    */
  def run(): SchedulerResult[A] = {
    // Was this computation interrupted? If there's a memoizer instance
    // restore thunks from scheduler trace.
    val thunks: List[Thunk[A]] = memo_opt match {
      case Some(memo) => memo.restore(question.memo_hash)
      case None => List.empty
    }

    // set initial conditions and then call the tail-recursive version
    run_tr(thunks, suffered_timeout = false)
  }

  @tailrec private def run_tr(thunks: List[Thunk[A]], suffered_timeout: Boolean) : SchedulerResult[A] = {
    val s = question.strategy_instance

    if(s.is_done(thunks)) {
      // pay for answers
      val all_thunks = Scheduler.accept_and_reject(s.thunks_to_accept(thunks), s.thunks_to_reject(thunks), backend)

      // calculate total cost
      val final_spent = Scheduler.total_cost(all_thunks)

      // return answer
      s.select_answer(thunks) match {
        case Some(result) => result
        case None => throw new Exception("The computation cannot both be completed and have no answer.")
      }
    } else {
      // get list of workers who may not re-participate
      val blacklist = s.blacklisted_workers(thunks)
      // post more tasks as needed
      val new_thunks = Scheduler.post_as_needed(thunks, backend, question, suffered_timeout, blacklist)
      // The scheduler waits here to give the crowd time to answer.
      // Sleeping also informs the scheduler that this thread may yield
      // its CPU time.
      Thread.sleep(poll_interval_in_s * 1000)
      // ask the backend to retrieve answers for all RUNNING thunks
      val (running_thunks,dead_thunks) = (thunks ::: new_thunks).partition(_.state == SchedulerState.RUNNING)
      assert(running_thunks.size > 0)
      val answered_thunks = backend.retrieve(running_thunks)
      assert(Scheduler.retrieve_invariant(running_thunks, answered_thunks))
      // complete list of thunks
      val all_thunks = answered_thunks ::: dead_thunks

      // memoize thunks
      memo_opt match {
        case Some(memo) => memo.save(all_thunks)
        case None => ()
      }
      // recursive call
      run_tr(all_thunks, Scheduler.timeout_occurred(answered_thunks))
    }
  }
}

object Scheduler {
  def cost_for_thunks(thunks: List[Thunk[_]]) : BigDecimal = {
    thunks.foldLeft(BigDecimal(0)) { case (acc, t) => acc + t.cost }
  }

  /**
   * Check to see whether a timeout occurred given a list of Thunks.
   * @param thunks A list of Thunks.
   * @tparam A The data type of the returned Answer.
   * @return True if at least one timeout occurred.
   */
  def timeout_occurred[A](thunks: List[Thunk[_]]) : Boolean = {
    thunks.count(_.state == SchedulerState.TIMEOUT) > 0
  }

  /**
   * Post new tasks if needed. Returns only newly-created thunks.
   * @param thunks The complete list of thunks.
   * @param question Question data.
   * @param suffered_timeout True if any thunks suffered a timeout on the last iteration.
   * @tparam A The data type of the returned Answer.
   * @return A list of newly-created Thunks.
   */
  def post_as_needed[A](thunks: List[Thunk[A]], backend: AutomanAdapter, question: Question[A], suffered_timeout: Boolean, blacklist: List[String]) : List[Thunk[A]] = {
    val s = question.strategy_instance
    
    // are any thunks still running?
    if (thunks.count(_.state == SchedulerState.RUNNING) == 0) {
      // no, so post more
      val new_thunks = s.spawn(thunks, suffered_timeout)
      assert(spawn_invariant(new_thunks))
      // can we afford these?
      if (question.budget < Scheduler.cost_for_thunks(thunks ::: new_thunks)) {
        throw new OverBudgetException[A](s.select_answer(thunks))
      } else {
        // yes, so post and return all posted thunks
        backend.post(new_thunks, blacklist)
      }
    } else {
      List.empty
    }
  }

  /**
   * Accepts and rejects tasks on the backend.  Returns all Thunks.
   * @param to_accept A list of Thunks to be accepted.
   * @param to_reject A list of Thunks to be rejected.
   * @tparam A The data type of the returned Answer.
   * @return The amount of money spent.
   */
  def accept_and_reject[A](to_accept: List[Thunk[A]], to_reject: List[Thunk[A]], backend: AutomanAdapter) : List[Thunk[A]] = {
    val accepted = to_accept.map(backend.accept)
    assert(all_set_invariant(to_accept, accepted, SchedulerState.ACCEPTED))
    val rejected = to_reject.map(backend.reject)
    assert(all_set_invariant(to_reject, rejected, SchedulerState.REJECTED))
    accepted ::: rejected
  }

  /**
   * Calculates the total cost of all ACCEPTED thunks.
   * @param thunks The complete list of Thunks.
   * @return The amount spent.
   */
  def total_cost(thunks: List[Thunk[_]]) : BigDecimal = {
    thunks.filter(_.state == SchedulerState.ACCEPTED).foldLeft(BigDecimal(0)) { case (acc,t) => acc + t.cost }
  }

  // INVARIANTS

  /**
   * Given a list of RUNNING thunks and a list of thunks returned from
   * the AutomanAdapter.retrieve method, ensure that a number of
   * invariants hold.
   * @param running A list of RUNNING thunks.
   * @param answered A list of thunks returned by the AutomanAdapter.retrieve method.
   * @tparam A The data type of the returned Answer.
   * @return True if all invariants hold.
   */
  def retrieve_invariant[A](running: List[Thunk[A]], answered: List[Thunk[A]]) : Boolean = {
    // all of the running thunks should actually be RUNNING
    running.count(_.state == SchedulerState.RUNNING) == running.size &&
      // the number of thunks given should be the same number returned
      answered.size == running.size &&
      // returned thunks should all either be RUNNING, RETRIEVED, or TIMEOUT
      answered.count { t =>
        t.state == SchedulerState.RUNNING ||
          t.state == SchedulerState.RETRIEVED ||
          t.state == SchedulerState.TIMEOUT
      } == running.size
  }

  /**
   * The list of newly-spawned thunks should never be zero.
   * @param new_thunks A list of newly-spawned thunks.
   * @tparam A The data type of the returned Answer.
   * @return True if the invariant holds.
   */
  def spawn_invariant[A](new_thunks: List[Thunk[A]]) : Boolean = {
    new_thunks.size != 0
  }

  /**
   * Returns true if all of the Thunks from the before list are set to the
   * given state in the after list.
   * @param before A list of Thunks.
   * @param after A list of Thunks.
   * @param state The state to check.
   * @tparam A The data type of the returned Answer.
   * @return True if the invariant holds.
   */
  def all_set_invariant[A](before: List[Thunk[A]], after: List[Thunk[A]], state: SchedulerState.Value) : Boolean = {
    val after_set = after.map { t => t.thunk_id }.toSet
    before.foldLeft(true){ case (acc,t) => acc && after_set.contains(t.thunk_id) }
  }
}