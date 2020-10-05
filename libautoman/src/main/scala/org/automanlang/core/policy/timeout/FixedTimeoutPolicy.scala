package org.automanlang.core.policy.timeout

import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.Task

class FixedTimeoutPolicy(question: Question) extends TimeoutPolicy(question) {
  /**
   * Calculate the task timeout given a question and the
   * worker timeout.
   * @param worker_timeout_in_s The worker timeout.
   * @return The new task timeout, in seconds
   */
  override def calculateTaskTimeout(worker_timeout_in_s: Int): Int = (question.question_timeout_multiplier * worker_timeout_in_s).toInt

  /**
   * Calculate the worker timeout given a question, the
   * tasks so far, and the round.
   * @param tasks The Tasks so far.
   * @param currentRound The current round.
   * @return The new worker timeout, in seconds.
   */
  override def calculateWorkerTimeout(tasks: List[Task], currentRound: Int, had_timeout: Boolean): Int = {
    question.initial_worker_timeout_in_s
  }
}
