package org.automanlang.core.policy.timeout

import org.automanlang.core.logging.{LogLevelInfo, DebugLog, LogLevel, LogType}
import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.{SchedulerState, Task}

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
   * @param currentRound The current round.
   * @return The new worker timeout, in seconds.
   */
  override def calculateWorkerTimeout(tasks: List[Task], currentRound: Int, had_timeout: Boolean): Int = {
    if (currentRound == 0 && tasks.isEmpty) {
      question.initial_worker_timeout_in_s
    } else {
      // get the tasks from the last round
      val last_round = tasks.filter(_.round == currentRound)

      // get the latest task's timeout
      val worker_timeout = last_round.sortBy(_.created_at).reverse.head.worker_timeout

      if (had_timeout) {
        DebugLog("Had a timeout; doubling worker timeout.", LogLevelInfo(), LogType.STRATEGY, question.id)
        worker_timeout * 2
      } else {
        worker_timeout
      }
    }
  }
}
