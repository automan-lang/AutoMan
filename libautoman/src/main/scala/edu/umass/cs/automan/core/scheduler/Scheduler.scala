package edu.umass.cs.automan.core.scheduler

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.info.{EpochInfo, QuestionInfo}
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.strategy.ValidationStrategy
import scala.collection.mutable
import scala.collection.mutable.Queue
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities, AutomanAdapter}
import edu.umass.cs.automan.core.exception.OverBudgetException

class Scheduler[A](val question: Question[A],
                   val adapter: AutomanAdapter,
                   val memoizer: Option[Memo],
                   val poll_interval_in_s: Int,
                   val virtual_time: Option[Date]) {
  def this(question: Question[A],
           adapter: AutomanAdapter,
           memoizer: Option[Memo],
           poll_interval_in_s: Int) = this(question, adapter, memoizer, poll_interval_in_s, None)
  // we need to get the initialized strategy instance from
  // the Question itself in order to satisfy the type checker
  private val _strategy: ValidationStrategy[A] = question.strategy_instance
  private var _thunks = scala.collection.immutable.Map[UUID,Thunk[A]]()

  def post_new_thunks(thunks: List[Thunk[A]], last_iteration_timeout: Boolean, memo_answers: Queue[BackendResult[A]]) : List[Thunk[A]] = {
    val num_answers = answered_thunks(thunks).size

    // spawn new thunks; in READY state here
    val new_thunks = _strategy.spawn(thunks, last_iteration_timeout) // OverBudgetException should be thrown here

    // before posting, pick answers out of memo DB
    val recalled_thunks = recall(new_thunks, memo_answers) // blocks

    // remaining new thunks
    val rem_new_thunks = new_thunks.filter{ t => recalled_thunks.forall(_.thunk_id != t.thunk_id)}

    // post remaining thunks
    val posted_thunks = if (!question.dry_run && rem_new_thunks.size > 0) {
      post(rem_new_thunks) // non-blocking
    } else {
      List.empty
    }

    recalled_thunks ::: posted_thunks
  }

  def run() : SchedulerResult[A] = {
    // check memo DB first
    val memo_answers = get_memo_answers()
    val unpaid_timeouts = new mutable.HashSet[Thunk[A]]()
    var last_iteration_timeout = false
    var over_budget = false
    Utilities.DebugLog("Found " + memo_answers.size + " saved Answers in database.", LogLevel.INFO, LogType.SCHEDULER, question.id)

    try {
      Utilities.DebugLog("Entering scheduling loop...", LogLevel.INFO, LogType.SCHEDULER, question.id)
      Utilities.DebugLog(String.format("Initial budget set at: %s", Utilities.decimalAsDollars(question.budget)), LogLevel.INFO, LogType.SCHEDULER, question.id)
      // run startup hook
      if(!question.dry_run) {
        adapter.question_startup_hook(question)
      }
      while(!_strategy.is_done(_thunks.values.toList)) {
        if (running_thunks(_thunks).size == 0) {
          synchronized {
            val new_thunks = post_new_thunks(_thunks.values.toList, last_iteration_timeout, memo_answers)

            // update thunk data structures
            _thunks = _thunks ++ new_thunks.map { t => t.thunk_id -> t}
          }
        }

        // The scheduler must wait, to give the crowd time to answer.
        // This also informs the scheduler that this thread may yield
        // its CPU time.
        Thread.sleep(poll_interval_in_s * 1000)

        // ask the backend for answers and memoize_answers
        // conditional covers the case where all thunk answers are recalled from memoDB
        if (running_thunks(_thunks).size > 0) {
          // get data
          val results =
            if (!question.dry_run) {
              adapter.retrieve(running_thunks(_thunks)) // blocks
            } else {
              List.empty
            }

          // atomically update thunk data structures with new results
          synchronized {
            _thunks = _thunks ++ results.map { t => t.thunk_id -> t}
          }

          // check for timed-out thunks
          if (results.count(_.state == SchedulerState.TIMEOUT) != 0) {
            // unreserve these amounts from our budget
            val new_timeouts = results.filter{ t =>
              t.state == SchedulerState.TIMEOUT &&
              !unpaid_timeouts.contains(t)
            }
            _strategy.unpay_for_thunks(new_timeouts)
            last_iteration_timeout = true
          } else {
            last_iteration_timeout = false
          }
          // saves *recently* RETRIEVED thunks
          memoize_answers(results.asInstanceOf[List[Thunk[A]]])  // blocks
        }
      }
      // run shutdown hook
      if(!question.dry_run) {
        adapter.question_shutdown_hook(question)
      }

    } catch {
      case o:OverBudgetException[_] => {
        Utilities.DebugLog("OverBudgetException.", LogLevel.FATAL, LogType.SCHEDULER, question.id)
        over_budget = true
      }
    }
    Utilities.DebugLog("Exiting scheduling loop...", LogLevel.INFO, LogType.SCHEDULER, question.id)

    synchronized {
      _strategy.select_answer(_thunks.values.toList) match {
        case Some(scheduler_result) =>
          val cost = if (!over_budget) {
            accept_and_reject(_thunks)
          } else {
            throw new OverBudgetException(Some(scheduler_result))
          }

          // log
          Utilities.DebugLog("Total spent: " + cost.toString(), LogLevel.INFO, LogType.SCHEDULER, question.id)

          // final result; will be wrapped in Answer[A] by caller
          scheduler_result
        case None => ???
      }
    }
  }

  def get_memo_answers() : Queue[BackendResult[A]] = {
    ???
//    val answers = Queue[A]()
//    answers ++= (memoizer match {
//      case Some(m) => m.checkDB(question).map{ a => a.asInstanceOf[A]}
//      case None => List()
//    })
//    answers
  }

  def accept_and_reject(ts: Map[UUID, Thunk[A]]) : BigDecimal = {
    var spent: BigDecimal = 0
    val accepts = _strategy.thunks_to_accept(ts.values.toList)
    val rejects = _strategy.thunks_to_reject(ts.values.toList)
    val cancels = ts.values.filter(_.state == SchedulerState.CANCELLED)
    val timeouts = ts.values.filter(_.state == SchedulerState.TIMEOUT)

    // Accept
    accepts.foreach { t =>
      Utilities.DebugLog(
        "Accepting thunk " + t.thunk_id +  " with answer \"" + t.answer.get.toString + "\" and paying $" +
          t.cost.toString(), LogLevel.INFO, LogType.SCHEDULER, question.id
      )
      val t2 = adapter.accept(t)
//      thunklog match {
//        case Some(tl) => tl.writeThunk(t2, SchedulerState.ACCEPTED, t2.worker_id.get)
//        case None => Unit
//      }
      ???
      spent += t.cost
    }

    // Reject
    if (!question.dont_reject) {
      rejects.foreach { t =>
        Utilities.DebugLog(
          "Rejecting thunk " + t.thunk_id + " with incorrect answer \"" +
            t.answer.get.toString + "\"", LogLevel.INFO, LogType.SCHEDULER, question.id
        )
        val t2 = adapter.reject(t)
//        thunklog match {
//          case Some(tl) => tl.writeThunk(t2, SchedulerState.REJECTED, t2.worker_id.get)
//          case None => Unit
//        }
        ???
      }
    } else {
      // Accept if the user specified "don't reject"
      rejects.foreach { t =>
        Utilities.DebugLog(
          "Accepting (NOT rejecting) thunk " + t.thunk_id + " with incorrect answer \"" +
            t.answer.get.toString + "\". If you did not want this, set Question.dont_reject to false.",
          LogLevel.INFO, LogType.SCHEDULER, question.id
        )
        val t2 = adapter.accept(t)
//        thunklog match {
//          case Some(tl) => tl.writeThunk(t2, SchedulerState.ACCEPTED, t2.worker_id.get)
//          case None => Unit
//        }
        ???
        spent += t2.cost
      }
    }

    // Early cancel
    cancels.foreach { t =>
      Utilities.DebugLog("Cancelling RUNNING thunk " + t.thunk_id + "...", LogLevel.INFO, LogType.SCHEDULER, question.id)
      adapter.cancel(t)
//      thunklog match {
//        case Some(tl) => tl.writeThunk(t, SchedulerState.CANCELLED, null)
//        case None => Unit
//      }
      ???
    }

    // Timeouts
    timeouts.foreach { t =>
//      thunklog match {
//        case Some(tl) => tl.writeThunk(t, SchedulerState.TIMEOUT, null)
//        case None => Unit
//      }
      ???
    }

    spent
  }

  def answered_thunks(ts: List[Thunk[A]]) : List[Thunk[A]] = ts.filter( t =>
    t.state == SchedulerState.PROCESSED &&
    t.state == SchedulerState.RETRIEVED ||
    t.state == SchedulerState.ACCEPTED ||
    t.state == SchedulerState.REJECTED
  ).toList
  def completed_thunks(ts: Map[UUID,Thunk[A]]) : List[Thunk[A]] = ts.values.filter( t =>
    t.state == SchedulerState.PROCESSED &&
    t.state == SchedulerState.RETRIEVED ||
    t.state == SchedulerState.ACCEPTED ||
    t.state == SchedulerState.REJECTED ||
    t.state == SchedulerState.TIMEOUT  ||
    t.state == SchedulerState.CANCELLED
  ).toList
  def incomplete_thunks(ts: Map[UUID,Thunk[A]]) : List[Thunk[A]] = ts.values.filter( t =>
    t.state != SchedulerState.PROCESSED &&
    t.state != SchedulerState.RETRIEVED &&
    t.state != SchedulerState.ACCEPTED &&
    t.state != SchedulerState.REJECTED &&
    t.state != SchedulerState.TIMEOUT &&
    t.state != SchedulerState.CANCELLED
  ).toList
  def memoize_answers(ts: List[Thunk[A]]) {
    // save
    memoizer match {
      case Some(m) =>
        ts.filter(_.state == SchedulerState.RETRIEVED).foreach {t =>
//          m.writeAnswer(question, t.answer.get)
          ???
        }
      case None => Unit
    }
  }
  def post(ts: List[Thunk[A]]) : List[Thunk[A]] = {
    Utilities.DebugLog("Posting " + ts.size + " tasks.", LogLevel.INFO, LogType.SCHEDULER, question.id)
    adapter.post(ts, question.blacklisted_workers)
  }
  def running_thunks(ts: Map[UUID,Thunk[A]]) = ts.values.filter(_.state == SchedulerState.RUNNING).toList
  def recall(ts: List[Thunk[A]], results: Queue[BackendResult[A]]) : List[Thunk[A]] = {
    // sanity check
    assert(ts.forall(_.state == SchedulerState.READY))

    if (results.size > 0) {
      ts.take(results.size).map { t =>
        val result = results.dequeue()
        Utilities.DebugLog("Pairing thunk " + t.thunk_id + " with memoized result \"" + result.toString + "\".", LogLevel.INFO, LogType.SCHEDULER, question.id)
        val worker_id = result.worker_id
        val t2 = t.copy_with_answer(result.answer, worker_id)
        t2.question.blacklist_worker(worker_id)
//        adapter.process_custom_info(t2, result.custom_info)
        ???
        t2
      }
    } else {
      List.empty
    }
  }
}