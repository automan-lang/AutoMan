package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.{DistributionAnswer, Answer}
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}
import edu.umass.cs.automan.core.question.DistributionQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

class DefaultDistributionStrategy[Q <: DistributionQuestion, A <: DistributionAnswer](question: Q, num_samples: Int = 30)
  extends DistributionValidationStrategy[Q,A](question) {
  def spawn(had_timeout: Boolean): List[Thunk[A]] = {
    // num to spawn
    val num_to_spawn = if (_thunks.filter(_.state == SchedulerState.RUNNING).size == 0) {
      num_samples
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
        throw OverBudgetException("")
      }
      val t = new Thunk[A](question, question.question_timeout_in_s, question.worker_timeout_in_s, question.reward, _computation_id)
      Utilities.DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY,_computation_id)
      t
    }.toList
    _thunks = new_thunks ::: _thunks

    new_thunks
  }

  override def select_answer: A = throw new NotImplementedError("Wait... working on it!")
}
