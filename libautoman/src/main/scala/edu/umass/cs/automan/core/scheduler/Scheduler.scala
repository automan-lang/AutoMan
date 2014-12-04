package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.memoizer.{ThunkLogger, AutomanMemoizer}
import edu.umass.cs.automan.core.answer.{Answer, ScalarAnswer}
import scala.collection.mutable
import scala.collection.mutable.Queue
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities, AutomanAdapter}
import edu.umass.cs.automan.core.exception.OverBudgetException

class Scheduler (val question: Question,
                 val adapter: AutomanAdapter,
                 val memoizer: AutomanMemoizer,
                 val thunklog: ThunkLogger,
                 val poll_interval_in_s: Int) {
  type A = question.A
  type B = question.B
  // we need to get the initialized strategy instance from
  // the Question itself in order to satisfy the type checker
  val strategy = question.strategy_instance
  var thunks = List[Thunk[A]]()

  def sch_post(last_iteration_timeout: Boolean, memo_answers: Queue[A]) : List[Thunk[A]] = {
    // spawn new thunks; in READY state here
    val new_thunks = strategy.spawn(last_iteration_timeout) // OverBudgetException should be thrown here

    // before posting, pick answers out of memo DB
    val recalled_thunks = recall(new_thunks, memo_answers) // blocks

    // post remaining thunks
    val posted_thunks = if (!question.dry_run) {
      post(new_thunks.filter{ t => !recalled_thunks.contains(t.thunk_id)}) // non-blocking
    } else {
      List.empty
    }

    recalled_thunks ::: posted_thunks
  }

  def run() : B = {
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
      while(!strategy.is_done) {
        if (running_thunks.size == 0) {
          val new_thunks = sch_post(last_iteration_timeout, memo_answers) ::: thunks
          synchronized {
            thunks = new_thunks ::: thunks
          }
        }

        // ask the backend for answers and memoize_answers
        // conditional covers the case where all thunk answers are recalled from memoDB
        if (running_thunks.size > 0) {
          // get data
          val results = if (!question.dry_run) {
            adapter.retrieve(running_thunks) // blocks
          } else { List[Thunk[A]]() }

          // check for timeout
          if (results.count(_.state == SchedulerState.TIMEOUT) != 0) {
            // unreserve these amounts from our budget
            val new_timeouts = results.filter{ t =>
              t.state == SchedulerState.TIMEOUT &&
              !unpaid_timeouts.contains(t)
            }
            strategy.unpay_for_thunks(new_timeouts)
            last_iteration_timeout = true
          } else {
            last_iteration_timeout = false
          }
          // saves *recently* RETRIEVED thunks
          memoize_answers(results.asInstanceOf[List[Thunk[A]]])  // blocks
        }

        // sleep if necessary
        // TODO: this sleep may no longer be necessary
        if (!strategy.is_done && incomplete_thunks(thunks).size > 0) Thread.sleep(poll_interval_in_s * 1000)
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

    val answer = strategy.select_answer
    var spent: BigDecimal = 0
    if (!over_budget) {
      spent = accept_and_reject(thunks, answer.asInstanceOf[B])
      Utilities.DebugLog("Total spent: " + spent.toString(),LogLevel.INFO, LogType.SCHEDULER, question.id)
    } else {
      throw new OverBudgetException(Some(answer))
    }
    question.final_cost = spent
    answer.asInstanceOf[B]
  }

  def get_memo_answers() : Queue[A] = {
    val answers = Queue[A]()
    answers ++= memoizer.checkDB(question).map{ a => a.asInstanceOf[A]}
    answers
  }

  def accept_and_reject(ts: List[Thunk[A]], answer: B) : BigDecimal = {
    var spent: BigDecimal = 0
    val accepts = strategy.thunks_to_accept
    val rejects = strategy.thunks_to_reject
    val cancels = ts.filter(_.state == SchedulerState.RUNNING)
    val timeouts = ts.filter(_.state == SchedulerState.TIMEOUT)

    // Accept
    accepts.foreach { t =>
      Utilities.DebugLog(
        "Accepting thunk with answer: " + t.answer.toString + " and paying $" +
          t.cost.toString(), LogLevel.INFO, LogType.SCHEDULER, question.id
      )
      adapter.accept(t)
      thunklog.writeThunk(t, SchedulerState.ACCEPTED, t.worker_id.get)
      spent += t.cost
    }

    // Reject
    if (!question.dont_reject) {
      rejects.foreach { t =>
        Utilities.DebugLog(
          "Rejecting thunk with incorrect answer: " +
            t.answer.toString, LogLevel.INFO, LogType.SCHEDULER, question.id
        )
        adapter.reject(t)
        thunklog.writeThunk(t, SchedulerState.REJECTED, t.worker_id.get)
      }
    } else {
      // Accept if the user specified "don't reject"
      rejects.foreach { t =>
        Utilities.DebugLog(
          "Accepting (NOT rejecting) thunk with incorrect answer " +
            "(if you did not want this, set  Question.dont_reject to false): " +
            t.answer.toString, LogLevel.INFO, LogType.SCHEDULER, question.id
        )
        adapter.accept(t)
        thunklog.writeThunk(t, SchedulerState.ACCEPTED, t.worker_id.get)
        spent += t.cost
      }
    }

    // Early cancel
    cancels.foreach { t =>
      Utilities.DebugLog("Cancelling RUNNING thunk...", LogLevel.INFO, LogType.SCHEDULER, question.id)
      adapter.cancel(t)
      thunklog.writeThunk(t, SchedulerState.CANCELLED, null)
    }

    // Timeouts
    timeouts.foreach { t =>
      thunklog.writeThunk(t, SchedulerState.TIMEOUT, null)
    }

    spent
  }

  def completed_thunks(ts: List[Thunk[_]]) : List[Thunk[_]] = ts.filter( t =>
    t.state == SchedulerState.PROCESSED &&
    t.state == SchedulerState.RETRIEVED ||
    t.state == SchedulerState.ACCEPTED ||
    t.state == SchedulerState.REJECTED ||
    t.state == SchedulerState.TIMEOUT
  )
  def incomplete_thunks(ts: List[Thunk[_]]) : List[Thunk[_]] = ts.filter( t =>
    t.state != SchedulerState.PROCESSED &&
    t.state != SchedulerState.RETRIEVED &&
    t.state != SchedulerState.ACCEPTED &&
    t.state != SchedulerState.REJECTED &&
    t.state != SchedulerState.TIMEOUT
  )
  def memoize_answers(ts: List[Thunk[A]]) {
    // save
    ts.filter(_.state == SchedulerState.RETRIEVED).foreach{t =>
      memoizer.writeAnswer(question, t.answer)
    }
  }
  def post(ts: List[Thunk[A]]) : List[Thunk[A]] = {
    Utilities.DebugLog("Posting " + ts.size, LogLevel.INFO, LogType.SCHEDULER, question.id)
    adapter.post(ts, question.blacklisted_workers)
  }
  def running_thunks = thunks.filter(_.state == SchedulerState.RUNNING)
  def recall(ts: List[Thunk[A]], answers: Queue[A]) : List[Thunk[A]] = {
    // sanity check
    assert(ts.forall(_.state == SchedulerState.READY))

    ts.map { t =>
      if(answers.size > 0) {
        Utilities.DebugLog("Pairing thunk with memoized answer.", LogLevel.INFO, LogType.SCHEDULER, question.id)
        val answer = answers.dequeue()
        val worker_id = answer.worker_id
        val t2 = t.copy_with_answer(answer, worker_id)
        t2.question.blacklist_worker(worker_id)
        adapter.process_custom_info(t2, answer.custom_info)
        t2
      } else {
        t
      }
    }
  }
}