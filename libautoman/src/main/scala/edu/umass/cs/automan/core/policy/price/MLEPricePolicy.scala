package edu.umass.cs.automan.core.policy.price

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

class MLEPricePolicy(question: Question) extends PricePolicy(question) {
  def calculateReward(tasks: List[Task], round: Int) : BigDecimal = {
    if (round == 1) {
      calculateInitialReward()
    } else {
      val last_round = tasks.filter(_.round == round - 1)
      val current_reward = last_round.head.cost
      // find all tasks scheduled at the current_reward
      val last_timeout_epoch = tasks.filter(_.cost == current_reward)

      val answered = last_timeout_epoch.filter(_.state != SchedulerState.TIMEOUT)
      if (answered.size == 0) {
        BigDecimal(2.0).max(current_reward)
      } else {
        val mean: BigDecimal = BigDecimal(answered.size) / BigDecimal(last_round.size)
        1.0 / mean * current_reward
      }
    }
  }
}
