package edu.umass.cs.automan.core.policy.timeout

import edu.umass.cs.automan.core.logging.{DebugLog, LogLevel, LogType}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

class DoublingTimeoutPolicy(question: Question) extends TimeoutPolicy(question) {
  /**
   * Calculate the task timeout given a question and the
   * worker timeout.
   * @param worker_timeout_in_s The worker timeout.
   * @return The new task timeout, in seconds
   */
  override def calculateTaskTimeout(worker_timeout_in_s: Int): Int = {
    (worker_timeout_in_s * question.question_timeout_multiplier).toInt
  }

  /**
   * Calculate the worker timeout given a question, the
   * tasks so far, and the round.
   * @param tasks The Tasks so far.
   * @param round The current round.
   * @return The new worker timeout, in seconds.
   */
  override def calculateWorkerTimeout(tasks: List[Task], round: Int, had_timeout: Boolean): Int = {
    if (round == 0 && tasks.size == 0) {
      question.initial_worker_timeout_in_s
    } else {
      // get the thunks from the last round
      val last_round = tasks.filter(_.round == round)

      if (had_timeout) {
        DebugLog("Had a timeout; doubling worker timeout.", LogLevel.INFO, LogType.STRATEGY, question.id)
        last_round.head.worker_timeout * 2
      } else {
        last_round.head.worker_timeout
      }
    }
  }
}
