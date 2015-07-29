package edu.umass.cs.automan.core.policy.price

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

class MLEPricePolicy(question: Question) extends PricePolicy(question) {
  def calculateReward(tasks: List[Task], round: Int, timeout_occurred: Boolean) : BigDecimal = {
    if (round == 1) {
      calculateInitialReward()
    } else {
      // find the last round where we spawned tasks
      val last_spawn_round = tasks.filter(_.round != round).map(_.round).max
      // get the thunks from that round
      val last_round = tasks.filter(_.round == last_spawn_round)
      val current_reward = last_round.head.cost

      if (timeout_occurred) {
        // find all tasks scheduled at the current_reward
        val last_timeout_epoch = tasks.filter(_.cost == current_reward)

        // get # unanswered tasks
        val unanswered = last_timeout_epoch.filter(_.state == SchedulerState.TIMEOUT)

        // # unanswered cannot be zero, otherwise a timeout would not have occurred
        assert(unanswered.size != 0)

        // use the MLE for the Bernoulli distribution (the mean) to
        // find the probability that a task will remain available (p_a)
        val p_a: BigDecimal = BigDecimal(unanswered.size) / BigDecimal(last_round.size)
        1.0 / p_a * current_reward
      } else {
        current_reward
      }
    }
  }
}
