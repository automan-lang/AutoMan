package edu.umass.cs.automan.core.policy.price

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.Task

abstract class PricePolicy(question: Question) {
  /**
   * Calculate the price given a question, its tasks, and the number of rounds.
   * @param tasks All elapsed tasks.
   * @param round Current round.
   * @return The price for the current round.
   */
  def calculateReward(tasks: List[Task], round: Int) : BigDecimal

  /**
   * Calculate the initial reward.
   * @return The price for the first round.
   */
  def calculateInitialReward() : BigDecimal = {
    (question.wage * question.initial_worker_timeout_in_s * (1.0/3600)).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
  }
}
