package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.ScalarAnswer
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}
import edu.umass.cs.automan.core.question.DistributionQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

class DefaultDistributionStrategy[Q <: DistributionQuestion, A <: ScalarAnswer, B](question: Q, num_samples: Int = 30)
  extends DistributionValidationStrategy[Q,A,B](question) {
  def outstanding_thunks =
    _thunks.filter(t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING ||
      t.state == SchedulerState.RETRIEVED ||
      t.state == SchedulerState.ACCEPTED ||
      t.state == SchedulerState.PROCESSED
    )

  def spawn(had_timeout: Boolean): List[Thunk[A]] = {
    // num to spawn
    val num_executing = outstanding_thunks.size
    val num_to_spawn = if (num_executing < num_samples) {
      num_samples - num_executing
    } else {
      return List[Thunk[A]]() // Be patient!
    }

    // determine duration
    if (had_timeout) {
      Utilities.DebugLog("Had a timeout; doubling worker timeout.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
      question.worker_timeout_in_s *= 2
    }

    Utilities.DebugLog("You should spawn " + num_to_spawn +
      " more Thunks at $" + question.reward + "/thunk, " +
      question.question_timeout_in_s + "s until question timeout, " +
      question.worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY,
      _computation_id)

    // allocate Thunk objects
    val new_thunks = (0 until num_to_spawn).map { i =>
      _budget_committed += question.reward
      if (_budget_committed > question.budget) {
        Utilities.DebugLog("Over budget. budget_committed = " + _budget_committed + " > budget = " + question.budget, LogLevel.FATAL, LogType.STRATEGY, _computation_id)
        throw OverBudgetException[A](None)
      }
      val t = new Thunk[A](question, question.question_timeout_in_s, question.worker_timeout_in_s, question.reward, _computation_id)
      Utilities.DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY,_computation_id)
      t
    }.toList
    _thunks = new_thunks ::: _thunks

    new_thunks
  }
}
