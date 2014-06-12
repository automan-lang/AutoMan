package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.strategy.ValidationStrategy
import edu.umass.cs.automan.core.memoizer.{ThunkLogger, AutomanMemoizer}
import edu.umass.cs.automan.core.answer.{CheckboxAnswer, RadioButtonAnswer, Answer}
import collection.mutable.Queue
import edu.umass.cs.automan.core.question.{CheckboxQuestion, RadioButtonQuestion, Question}
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities, AutomanAdapter}
import edu.umass.cs.automan.core.exception.{ExceedsMaxReplicas, OverBudgetException}

class Scheduler (val question: Question,
                 val adapter: AutomanAdapter[_,_,_],
                 val strategy: ValidationStrategy,
                 val memoizer: AutomanMemoizer,
                 val thunklog: ThunkLogger,
                 val poll_interval_in_s: Int) {
  var thunks = List[Thunk]()

  def run[A <: Answer]() : A = {
    // check memo DB first
    val memo_answers = get_memo_answers(is_dual = false)
    val memo_dual_answers = get_memo_answers(is_dual = true)
    var last_iteration_timeout = false
    var over_budget = false
    Utilities.DebugLog("Found " + (memo_answers.size + memo_dual_answers.size) + " saved Answers in database.", LogLevel.INFO, LogType.SCHEDULER, question.id)

    try {
      Utilities.DebugLog("Entering scheduling loop...", LogLevel.INFO, LogType.SCHEDULER, question.id)
      // run startup hook
      if(!question.dry_run) {
        adapter.question_startup_hook(question)
      }
      while(!strategy.is_confident) {
        if (thunks.filter(_.state == SchedulerState.RUNNING).size == 0) {
          // spawn new thunks; in READY state here
          val new_thunks = strategy.spawn(question, last_iteration_timeout) // OverBudgetException should be thrown here
          thunks = new_thunks ::: thunks

          // before posting, pick answers out of memo DB
          recall(new_thunks, memo_answers, memo_dual_answers) // blocks

          // only post READY state thunks to backend; become RUNNING state here
          if (!question.dry_run) {
            post(new_thunks.filter(_.state == SchedulerState.READY)) // non-blocking
          }
        }

        // ask the backend for answers and memoize_answers
        // conditional covers the case where all thunk answers are recalled from memoDB
        if (running_thunks.size > 0) {
          // get data
          val results = if (!question.dry_run) {
            adapter.retrieve(running_thunks) // blocks
          } else { List[Thunk]() }

          // check for timeout
          if (results.filter(_.state == SchedulerState.TIMEOUT).size != 0) {
            last_iteration_timeout = true
          } else {
            last_iteration_timeout = false
          }
          // saves *recently* RETRIEVED thunks
          memoize_answers(results)  // blocks
        }

        // sleep if necessary
        // TODO: this sleep may no longer be necessary
        if (!strategy.is_confident && incomplete_thunks(thunks).size > 0) Thread.sleep(poll_interval_in_s * 1000)
      }
      // run shutdown hook
      if(!question.dry_run) {
        adapter.question_shutdown_hook(question)
      }

    } catch {
      case o:OverBudgetException => {
        Utilities.DebugLog("OverBudgetException.", LogLevel.FATAL, LogType.SCHEDULER, question.id)
        over_budget = true
      }
    }
    Utilities.DebugLog("Exiting scheduling loop...", LogLevel.INFO, LogType.SCHEDULER, question.id)

    val answer = strategy.select_best(question).asInstanceOf[A]
    var spent: BigDecimal = 0
    if (!over_budget) {
      spent = accept_and_reject(thunks, answer)
      Utilities.DebugLog("Total spent: " + spent.toString(),LogLevel.INFO, LogType.SCHEDULER, question.id)
    } else {
      answer.over_budget = true
    }
    answer.final_cost
    answer
  }

  def get_memo_answers[A <: Answer](is_dual: Boolean) : Queue[A] = {
    val answers = Queue[A]()
    answers ++= memoizer.checkDB(question, is_dual).map{ a => a.asInstanceOf[A]}
    answers
  }

  // returns total reward
  def accept_and_reject[A <: Answer](ts: List[Thunk], answer: A) : BigDecimal = {
    var spent: BigDecimal = 0
    ts.filter(_.state == SchedulerState.RETRIEVED).foreach { t =>
      if(t.answer.comparator == answer.comparator) {
        Utilities.DebugLog("Accepting thunk with answer: " + t.answer.comparator.toString + " and paying $" + t.cost.toString(), LogLevel.INFO, LogType.SCHEDULER, question.id)
        adapter.accept(t)
        thunklog.writeThunk(t, SchedulerState.ACCEPTED, t.answer.worker_id)
        spent += t.cost
      } else if(!t.question.dont_reject) {
        Utilities.DebugLog("Rejecting thunk with incorrect answer: " + t.answer.comparator.toString, LogLevel.INFO, LogType.SCHEDULER, question.id)
        adapter.reject(t)
        thunklog.writeThunk(t, SchedulerState.REJECTED, t.answer.worker_id)
      } else {
        Utilities.DebugLog("Accepting (NOT rejecting) thunk with incorrect answer (if you did not want this, set  Question.dont_reject to false): " + t.answer.comparator.toString, LogLevel.INFO, LogType.SCHEDULER, question.id)
        adapter.accept(t)
        thunklog.writeThunk(t, SchedulerState.REJECTED, t.answer.worker_id)
        spent += t.cost
      }
    }
    ts.filter(_.state == SchedulerState.RUNNING).foreach { t =>
      Utilities.DebugLog("Cancelling RUNNING thunk...", LogLevel.INFO, LogType.SCHEDULER, question.id)
      adapter.cancel(t)
      thunklog.writeThunk(t, SchedulerState.CANCELLED, null)
    }
    ts.filter(_.state == SchedulerState.TIMEOUT).foreach { t =>
      thunklog.writeThunk(t, SchedulerState.TIMEOUT, null)
    }
    answer.final_cost = spent
    spent
  }
  def completed_thunks(ts: List[Thunk]) : List[Thunk] = ts.filter( t =>
    t.state == SchedulerState.PROCESSED &&
    t.state == SchedulerState.RETRIEVED ||
    t.state == SchedulerState.ACCEPTED ||
    t.state == SchedulerState.REJECTED ||
    t.state == SchedulerState.TIMEOUT
  )
  def incomplete_thunks(ts: List[Thunk]) : List[Thunk] = ts.filter( t =>
    t.state != SchedulerState.PROCESSED &&
    t.state != SchedulerState.RETRIEVED &&
    t.state != SchedulerState.ACCEPTED &&
    t.state != SchedulerState.REJECTED &&
    t.state != SchedulerState.TIMEOUT
  )
  def memoize_answers(ts: List[Thunk]) {
    // save
    ts.filter(_.state == SchedulerState.RETRIEVED).foreach{t =>
      memoizer.writeAnswer(question, t.answer, t.is_dual)
    }
  }
  def post(ts: List[Thunk]) {
    if (ts.filter(_.is_dual == true).size > 0) {
      Utilities.DebugLog("Posting " + ts.filter(_.is_dual == true).size + " dual = true", LogLevel.INFO, LogType.SCHEDULER, question.id)
      adapter.post(ts.filter(_.is_dual == true), true, question.blacklisted_workers)
    }
    if (ts.filter(_.is_dual == false).size > 0) {
      Utilities.DebugLog("Posting " + ts.filter(_.is_dual == false).size + " dual = false", LogLevel.INFO, LogType.SCHEDULER, question.id)
      adapter.post(ts.filter(_.is_dual == false), false, question.blacklisted_workers)
    }
  }
  def running_thunks = thunks.filter(_.state == SchedulerState.RUNNING)
  def recall[A <: Answer](ts: List[Thunk], answers: Queue[A], dual_answers: Queue[A]) {
    ts.filter(_.state == SchedulerState.READY).foreach { t =>
      if (t.is_dual) {
        if(dual_answers.size > 0) {
          Utilities.DebugLog("Pairing thunk with memoized answer.", LogLevel.INFO, LogType.SCHEDULER, question.id)
          t.answer = dual_answers.dequeue()
          t.state = SchedulerState.RETRIEVED
          t.question.blacklist_worker(t.answer.worker_id)
          adapter.process_custom_info(t, t.answer.custom_info)
        }
      } else {
        if(answers.size > 0) {
          Utilities.DebugLog("Pairing thunk with memoized answer.", LogLevel.INFO, LogType.SCHEDULER, question.id)
          t.answer = answers.dequeue()
          t.state = SchedulerState.RETRIEVED
          t.question.blacklist_worker(t.answer.worker_id)
          adapter.process_custom_info(t, t.answer.custom_info)
        }
      }
    }
  }
}