package edu.umass.cs.automan.core.policy.price

import edu.umass.cs.automan.core.logging.{LogType, LogLevelDebug, DebugLog}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

class MLEPricePolicy(question: Question) extends PricePolicy(question) {
  private def numAnswered(ts: List[Task]) : Int = {
    ts.count { t =>
      t.state == SchedulerState.ANSWERED ||
      t.state == SchedulerState.ACCEPTED ||
      t.state == SchedulerState.REJECTED ||
      t.state == SchedulerState.DUPLICATE
    }
  }

  private def numTimeouts(ts: List[Task]) : Int = {
    // find the last round where we spawned tasks
    val last_spawn_round = ts.map(_.round).max
    // get the tasks from that round
    val last_round = ts.filter(_.round == last_spawn_round)

    // get # timed-out tasks
    last_round.count { t =>
      t.state == SchedulerState.TIMEOUT || t.state == SchedulerState.CANCELLED
    }
  }

  def calculateReward(tasks: List[Task], round: Int, timeout_occurred: Boolean) : BigDecimal = {
    // first round, base case
    if (round == 0 && tasks.isEmpty) {
      val reward = calculateInitialReward()
      DebugLog(s"Initial reward is $$$reward. Round = $round.", LogLevelDebug(), LogType.STRATEGY, question.id)
      reward
    // the normal-state-of-affairs case
    } else if (numAnswered(tasks) != 0) {
      // if timeouts occur, a round will contain mixed prices; take the max
      val current_reward = tasks.map(_.cost).max

      if (timeout_occurred) {
        // # unanswered in last roundcannot be zero,
        // otherwise a timeout would not have occurred
        assert(numTimeouts(tasks) != 0)

        val num_answered = numAnswered(tasks)

        // Use the MLE for the Bernoulli distribution (the mean) to
        // find the probability that a task will be accepted (p_a).
        // We assume that p_a is a fixed population parameter unaffected by price.
        val p_a: BigDecimal = BigDecimal(num_answered) / BigDecimal(tasks.size)
        // maximal safe growth rate; see CACM paper.
        val growth_rate: BigDecimal = 1.0 / p_a
        val reward = (growth_rate * current_reward).setScale(2, math.BigDecimal.RoundingMode.FLOOR)

        DebugLog(s"Timeout occurred. New reward is $$$reward because the estimated acceptance " +
                 s"rate is $p_a per round and the current reward is $$$current_reward. Round = $round.",
                 LogLevelDebug(),
                 LogType.STRATEGY,
                 question.id)

        reward
      } else {
        DebugLog(s"Insufficient agreement. Keeping reward of $$$current_reward. Round = $round.",
          LogLevelDebug(),
          LogType.STRATEGY,
          question.id)

        current_reward
      }
    // the your-task-sucks-so-badly-nobody-will-take-it case
    } else {
      // if timeouts occur, a round will contain mixed prices; take the max
      val current_reward = tasks.map(_.cost).max

      // double the reward
      val reward = (2.0 * current_reward).setScale(2, math.BigDecimal.RoundingMode.FLOOR)

      DebugLog(s"Timeout occurred. New reward is $$$reward because we cannot estimate acceptance " +
               s"rate and the current reward is $$$current_reward. Round = $round.",
               LogLevelDebug(),
               LogType.STRATEGY,
               question.id)

      reward
    }
  }
}
