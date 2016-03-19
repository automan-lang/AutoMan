package edu.umass.cs.automan.core.scheduler

import java.util.{UUID, Date}
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.exception.{BackendFailureException, OverBudgetException}
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.policy.aggregation.AggregationPolicy
import edu.umass.cs.automan.core.util.Stopwatch

/**
 * Controls scheduling of tasks for a given question.
 *
 * @param question
 * @param backend
 */
class Scheduler(val question: Question,
                val backend: AutomanAdapter) {
  // save startup time
  val VIRT_FREQ = 1 // ms
  val init_time = new Date()
  val use_virt = question.mock_answers.nonEmpty

  // init policies
  question.init_validation_policy()
  question.init_price_policy()
  question.init_timeout_policy()

  val VP = question.validation_policy_instance

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

    DebugLog(
      "Found " + tasks.size + " saved Tasks in database with " +
      tasks.count(_.answer.isDefined) + " answers.", LogLevelInfo(), LogType.SCHEDULER, question.id
    )

    // set initial conditions and call scheduler loop
    run_loop(tasks)
  }

  private def taskStatus(ts: List[Task]) : Map[UUID,Date] = {
    ts.map { t => t.task_id -> t.state_changed_at }.toMap
  }

  private def taskInserts(newMap: Map[UUID,Date], oldMap: Map[UUID,Date]) : List[UUID] = {
    newMap
      .filter { case (uuid,_) => !oldMap.contains(uuid) }
      .map { case (uuid,_) => uuid }.toList
  }

  private def taskUpdates(newMap: Map[UUID,Date], oldMap: Map[UUID,Date]) : List[UUID] = {
    newMap
      .filter { case (uuid,date) => oldMap.contains(uuid) && date.after(oldMap(uuid)) }
      .map { case (uuid,_) => uuid}.toList
  }

  object Sch {
    private def schTimeouts(allTasks: List[Task], time: Time, num_comparisons: Int) : (List[Task],Boolean,Time,Int) = {
      val (ok, sufferedTimeout) = process_timeouts(allTasks, time.current_time)
      (ok, sufferedTimeout, time, num_comparisons)
    }

    private def schPost(tasks: List[Task], sufferedTimeout: Boolean, time: Time, num_comparisons: Int) : (List[Task],List[Task],Time,Int,Boolean) = {
      // get list of workers who may not re-participate
      val __blacklist = VP.blacklisted_workers(tasks)

      // post more tasks as needed
      val (__new_tasks, num_comparisons2, done) = post_as_needed(tasks, backend, question, sufferedTimeout, __blacklist, num_comparisons)

      // update _time with time of future timeouts
      val time2 = if (use_virt) {
        time.addTimeoutsFor(__new_tasks)
      } else {
        time
      }

      val (running, unrunning) = (tasks ::: __new_tasks).partition(_.state == SchedulerState.RUNNING)

      assert(if (!sufferedTimeout) { running.nonEmpty } else { true })

      (running, unrunning, time2, num_comparisons2, done)
    }

    private def schRetrieve(running: List[Task], unrunning: List[Task], time: Time, num_comparisons: Int, done: Boolean) : (List[Task],List[Task],Time,Int,Boolean) = {
      if (done) {
        DebugLog(
          "Done; skipping retrieve for " + running.size + " running tasks from backend.",
          LogLevelInfo(),
          LogType.SCHEDULER,
          question.id
        )

        (running, unrunning, time, num_comparisons, done)
      } else {
        DebugLog(
          "Retrieving answers for " + running.size + " running tasks from backend.",
          LogLevelInfo(),
          LogType.SCHEDULER,
          question.id
        )

        val __answered_tasks = failUnWrap(backend.retrieve(running, time.current_time))
        assert(retrieve_invariant(running, __answered_tasks))

        (__answered_tasks, unrunning, time, num_comparisons, done)
      }
    }

    private def schDeDup(answered: List[Task], unrunning: List[Task], time: Time, num_comparisons: Int, done: Boolean) : (List[Task],Time,Int,Boolean) = {
      if (done) {
        (answered ::: unrunning, time, num_comparisons, done)
      } else {
        val (nonDupes, dupes) = VP.partition_duplicates(answered ::: unrunning)
        // mark dupes as duplicate
        val allTasks = nonDupes ::: (
          if (dupes.nonEmpty) {
            failUnWrap(backend.cancel(dupes, SchedulerState.DUPLICATE))
          } else {
            Nil
          } )
        (allTasks, time, num_comparisons, done)
      }
    }

    private def schIncrTime(allTasks: List[Task], time: Time, num_comparisons: Int, done: Boolean) : (List[Task],Time,Int,Boolean) = {
      (allTasks, time.incrTime(),num_comparisons,done)
    }

    private val timeout = (schTimeouts _).tupled
    private val post = (schPost _).tupled
    private val retrieve = (schRetrieve _).tupled
    private val dedup = (schDeDup _).tupled
    private val incrtime = (schIncrTime _).tupled

    val marshal =
      timeout andThen   // process timeouts
      post andThen      // post new tasks
      retrieve andThen  // retrieve task answers
      dedup andThen     // deduplicate
      incrtime          // advance clock
  }

  private def run_loop(tasks: List[Task]) : Question#AA = {
    // initialize loop
    val _update_frequency_ms = if (use_virt) { question.update_frequency_ms } else { VIRT_FREQ }
    var _time = Time.incrTime(use_virt)(init_time)(Time.initTickQueue(init_time, question.mock_answers))
    var _all_tasks = tasks
    var _num_comparisons = 1
    var _done = false
    // if we have restored tasks from memo DB
    // check to see if we're done, incrementing
    // num_comparisons as appropriate.
    if (_all_tasks.nonEmpty) {
      val (done, nc) = question.validation_policy_instance.is_done(_all_tasks, _num_comparisons) // check for restored memo tasks
      _done = done
      _num_comparisons = nc
    }

    // process until done
    val answer = try {
      while(!_done) {
        val __duration = Stopwatch {
          DebugLog("Scheduler time is " + Time.format(_time.current_time) + ".", LogLevelInfo(), LogType.SCHEDULER, question.id)

          // marshal and unmarshal from backend
          val (__tasks, __time, __num_comparisons, __done) = Sch.marshal(_all_tasks, _time, _num_comparisons)

          // update state
          _all_tasks = __tasks
          _time = __time
          _num_comparisons = __num_comparisons
          _done = __done
        }

        // if we haven't already decided that we're done and either
        // 1. all the tasks are complete, or
        // 2. we allow early termination checks
        // check to see if we are done.
        if (!_done) {
          val all_tasks_completed = VP.outstanding_tasks(_all_tasks).isEmpty
          val early_termination_OK = VP.allow_early_termination()
          if (all_tasks_completed || early_termination_OK) {
            val (__done, __nc) = VP.is_done(_all_tasks, _num_comparisons)
            _done = __done
            _num_comparisons = __nc
          }
        }

        // sleep if this loop iteration < update_frequency_ms
        // prevents flooding worker with requests
        if (!_done && __duration.duration_ms < _update_frequency_ms) {
          val t = _update_frequency_ms - __duration.duration_ms
          DebugLog("Putting scheduler to sleep for " + t + " ms.", LogLevelDebug(), LogType.SCHEDULER, question.id)
          Thread.sleep(t)
        }
      }

      // select answer
      val answer = VP.select_answer(_all_tasks, _num_comparisons)

      // pay for answers
      _all_tasks = accept_reject_and_cancel(_all_tasks, VP, backend)

      // return answer
      answer
    } catch {
      case o: OverBudgetException =>
        val answer = VP.select_over_budget_answer(_all_tasks, o.need, o.have, _num_comparisons)
        // make sure workers are paid when failures occur
        _all_tasks = if (question.pay_all_on_failure) { accept_on_failure(_all_tasks, VP, backend) } else { _all_tasks }
        answer
    }

    // run shutdown hook
    backend.question_shutdown_hook(question)

    answer
  }

  def process_timeouts(ts: List[Task], current_time: Date) : (List[Task],Boolean) = {
    // find all timeouts
    val (timeouts,otherwise) = ts.partition { t =>
      t.state == SchedulerState.RUNNING &&
      t.is_timedout(current_time)
    }
    if (timeouts.nonEmpty) {
      DebugLog("Cancelling " + timeouts.size + " timed-out tasks.", LogLevelInfo(), LogType.SCHEDULER, question.id)
      // cancel and make state TIMEOUT
      val timed_out = failUnWrap(backend.cancel(timeouts, SchedulerState.TIMEOUT))
      // return all updated Task objects and signal whether timeout occurred
      (timed_out ::: otherwise, timeouts.nonEmpty)
    } else {
      (ts, false)
    }
  }

  /**
   * Post new tasks if needed. Returns only newly-created tasks.
   * @param tasks The complete list of tasks.
   * @param question Question data.
   * @param suffered_timeout True if any tasks suffered a timeout on the last iteration.
   * @param num_comparisons The number of times we've called is_done, inclusive
   * @return A list of newly-created tasks, the number of comparisons, and whether we're done
   */
  def post_as_needed(tasks: List[Task],
                     backend: AutomanAdapter,
                     question: Question,
                     suffered_timeout: Boolean,
                     blacklist: List[String],
                     num_comparisons: Int) : (List[Task],Int,Boolean) = {
    val s = question.validation_policy_instance

    // if we suffered a timeout, we might as well
    // check to see if we can terminate early
    val (done, num_comparisons2) = if (suffered_timeout) {
      VP.is_done(tasks, num_comparisons)
    } else {
      // no timeout, just keep going
      (false, num_comparisons)
    }

    // are any tasks still running?
    if (!done && tasks.count(_.state == SchedulerState.RUNNING) == 0) {
      // no, so post more
      // compute set of new tasks
      val new_tasks = s.spawn(tasks, suffered_timeout, num_comparisons2)
      assert(spawn_invariant(new_tasks))
      // can we afford these?
      val cost = total_cost(tasks ::: new_tasks)
      if (question.budget < cost) {
        // no
        DebugLog("Over budget. Need: " + cost.toString() + ", have: " + question.budget.toString(), LogLevelWarn(), LogType.SCHEDULER, question.id)
        throw new OverBudgetException(cost, question.budget)
      } else {
        // yes, so post and return all posted tasks
        val posted = failUnWrap(backend.post(new_tasks, blacklist))
        DebugLog("Posting " + posted.size + " tasks to backend.", LogLevelInfo(), LogType.SCHEDULER, question.id)
        (posted, num_comparisons2, done)
      }
    } else {
      // post nothing if either
      // 1. we had a timeout but answers are sufficient to terminate early, or
      // 2. we still have tasks running and should just wait
      (List.empty, num_comparisons2, done)
    }
  }

  def accept_on_failure[A](all_tasks: List[Task],
                           strategy: AggregationPolicy,
                           backend: AutomanAdapter) : List[Task] = {
    val to_cancel = strategy.tasks_to_cancel(all_tasks)
    val to_accept = strategy.tasks_to_accept_on_failure(all_tasks)

    assert(accept_invariant(to_accept), to_accept.map(_.state).mkString(", "))

    val correct_answer = strategy.rejection_response(to_accept)

    val action_items = to_cancel ::: to_accept

    val remaining_tasks = all_tasks.filterNot(action_items.contains(_))

    assert(mutex_invariant(to_cancel, to_accept, List.empty, remaining_tasks),
      (to_cancel ::: to_accept ::: remaining_tasks).map(_.task_id).mkString(", ")
    )

    val cancelled = if (to_cancel.nonEmpty) { failUnWrap(backend.cancel(to_cancel, SchedulerState.CANCELLED)) } else { List.empty }
    assert(all_set_invariant(to_cancel, cancelled, SchedulerState.CANCELLED))
    val accepted = if (to_accept.nonEmpty) { failUnWrap(backend.accept(to_accept)) } else { List.empty }
    assert(all_set_invariant(to_accept, accepted, SchedulerState.ACCEPTED))
    remaining_tasks ::: cancelled ::: accepted
  }

  /**
   * Accepts and rejects tasks on the backend.  Returns all tasks.
   * @param all_tasks All tasks.
   * @param strategy The ValidationStrategy.
   * @param backend A reference to the backend AutomanAdapter.
   * @return The tasks passed in, with new states.
   */
  def accept_reject_and_cancel[A](all_tasks: List[Task],
                                  strategy: AggregationPolicy,
                                  backend: AutomanAdapter) : List[Task] = {
    val to_cancel = strategy.tasks_to_cancel(all_tasks)
    val to_accept = strategy.tasks_to_accept(all_tasks)
    val to_reject = strategy.tasks_to_reject(all_tasks)

    assert(accept_invariant(to_accept), to_accept.map(_.state).mkString(", "))
    assert(reject_invariant(to_reject), to_reject.map(_.state).mkString(", "))

    val correct_answer = strategy.rejection_response(to_accept)

    val action_items = to_cancel ::: to_accept ::: to_reject

    val remaining_tasks = all_tasks.filterNot(action_items.contains(_))

    assert(mutex_invariant(to_cancel, to_accept, to_reject, remaining_tasks),
      (to_cancel ::: to_accept ::: to_reject ::: remaining_tasks).map(_.task_id).mkString(", ")
    )

    val cancelled = if (to_cancel.nonEmpty) { failUnWrap(backend.cancel(to_cancel, SchedulerState.CANCELLED)) } else { List.empty }
    assert(all_set_invariant(to_cancel, cancelled, SchedulerState.CANCELLED))
    val accepted = if (to_accept.nonEmpty) { failUnWrap(backend.accept(to_accept)) } else { List.empty }
    assert(all_set_invariant(to_accept, accepted, SchedulerState.ACCEPTED))
    val rejected = if (to_reject.nonEmpty) { failUnWrap(backend.reject(to_reject.map { t => (t,correct_answer) })) } else { List.empty }
    assert(all_set_invariant(to_reject, rejected, SchedulerState.REJECTED))
    remaining_tasks ::: cancelled ::: accepted ::: rejected
  }

  def accept_invariant(ts: List[Task]) : Boolean = {
    ts.forall { t =>
      t.state == SchedulerState.ANSWERED || // the typical case
      t.state == SchedulerState.DUPLICATE // in case we accept dupes
    }
  }

  def reject_invariant(ts: List[Task]) : Boolean = {
    ts.forall { t =>
      t.state == SchedulerState.ANSWERED || // the typical case
      t.state == SchedulerState.DUPLICATE   // in case we do not accept dupes
    }
  }

  def mutex_invariant(to_cancel: List[Task], to_accept: List[Task], to_reject: List[Task], remaining: List[Task]) : Boolean = {
    (to_cancel ::: to_accept ::: to_reject ::: remaining).foldLeft(Set[UUID](), true) {
      case ((uSet, acc), t) =>
        if (uSet.contains(t.task_id)) {
          (uSet, false)
        } else {
          (uSet + t.task_id, acc)
        }
    }._2
  }

  def failUnWrap(tso: Option[List[Task]]) : List[Task] = {
    tso match {
      case Some(ts) => ts
      case None => throw BackendFailureException()
    }
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
//      t.state != SchedulerState.DUPLICATE &&
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
  def spawn_invariant[A](new_tasks: List[Task]) : Boolean = new_tasks.nonEmpty

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