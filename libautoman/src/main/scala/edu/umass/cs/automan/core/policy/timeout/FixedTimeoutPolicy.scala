package edu.umass.cs.automan.core.policy.timeout

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.Task

class FixedTimeoutPolicy(question: Question) extends TimeoutPolicy(question) {
  /**
   * Calculate the task timeout given a question and the
   * worker timeout.
   * @param worker_timeout_in_s The worker timeout.
   * @return The new task timeout, in seconds
   */
  override def calculateTaskTimeout(worker_timeout_in_s: Int): Int = ???

  /**
   * Calculate the worker timeout given a question, the
   * tasks so far, and the round.
   * @param tasks The Tasks so far.
   * @param round The current round.
   * @return The new worker timeout, in seconds.
   */
  override def calculateWorkerTimeout(tasks: List[Task], round: Int): Int = {
    if (round == 1) {

    }
  }
}
