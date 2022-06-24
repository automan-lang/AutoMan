package org.automanlang.core.policy.price

import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.Task

abstract class PricePolicy(question: Question) {
  /**
   * Calculate the price given a question, its tasks, and the number of rounds.
   * @param tasks All elapsed tasks.
   * @param currentRound Current round.
   * @return The price for the current round.
   */
  def calculateReward(tasks: List[Task], currentRound: Int, timeout_occurred: Boolean) : BigDecimal

  /**
   * Calculate the initial reward.
   *
   * It is done by multiplying the hourly wage with the initial worker timeout
   * (both are constants set in question). We make the assumption that worker
   * takes at most the worker timeout time and will be paid the full wage when
   * they finish.
   *
   * @return The price for the first round.
   */
  def calculateInitialReward() : BigDecimal = {
    (question.wage * question.initial_worker_timeout_in_s * (1.0/3600)).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
  }
}
