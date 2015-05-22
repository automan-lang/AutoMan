package edu.umass.cs.automan.core.policy.price

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.Task

class FixedPricePolicy(question: Question) extends PricePolicy(question) {
  override def calculateReward(tasks: List[Task], round: Int): BigDecimal = {
    calculateInitialReward()
  }
}
