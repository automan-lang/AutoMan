package edu.umass.cs.automan.core.policy.price

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

class MLEPricePolicy(question: Question) extends PricePolicy(question) {
  def calculateReward(tasks: List[Task], round: Int, timeout_occurred: Boolean) : BigDecimal = {
    if (round == 0 && tasks.size == 0) {
      calculateInitialReward()
    } else {
      // find the last round where we spawned tasks
      val last_spawn_round = tasks.map(_.round).max
      // get the tasks from that round
      val last_round = tasks.filter(_.round == last_spawn_round)
      // if timeouts occur, a round will contain mixed prices; take the max
      val current_reward = last_round.map(_.cost).max

      if (timeout_occurred) {
        // get # unanswered tasks
        val num_unanswered = last_round.count(_.state == SchedulerState.TIMEOUT)

        // # unanswered cannot be zero, otherwise a timeout would not have occurred
        assert(num_unanswered != 0)

        // use the MLE for the Bernoulli distribution (the mean) to
        // find the probability that a task will remain available (p_a)
        val p_a: BigDecimal = BigDecimal(num_unanswered) / BigDecimal(last_round.size)
        (1.0 / p_a * current_reward).setScale(2, math.BigDecimal.RoundingMode.FLOOR)
      } else {
        current_reward
      }
    }
  }
}
