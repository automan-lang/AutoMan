package org.automanlang.core.policy.price

import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.Task

class FixedPricePolicy(question: Question) extends PricePolicy(question) {
  override def calculateReward(tasks: List[Task], currentRound: Int, timeout_occurred: Boolean): BigDecimal = {
    calculateInitialReward()
  }
}
