package edu.umass.cs.automan.core.policy.price

import edu.umass.cs.automan.core.logging.{LogType, LogLevelDebug, DebugLog}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import edu.umass.cs.automan.core.policy._

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

  private def initialReward(tasks: List[Task]) : BigDecimal = {
    val reward = calculateInitialReward()
    DebugLog(s"Initial reward is $$$reward. Round = ${nextRound(tasks, suffered_timeout = false)}.", LogLevelDebug(), LogType.STRATEGY, question.id)
    reward
  }

  private def timeoutReward(tasks: List[Task]) : BigDecimal = {
    val current_reward = currentReward(tasks)

    // # unanswered in last roundcannot be zero,
    // otherwise a timeout would not have occurred
    assert(numTimeouts(tasks) != 0)

    val num_answered = numAnswered(tasks)

    // Use the MLE for the Bernoulli distribution (the mean) to
    // find the probability that a task will be accepted (p_a).
    // We assume that p_a is a fixed population parameter unaffected by price.
    val p_a: BigDecimal = BigDecimal(num_answered) / BigDecimal(tasks.size)
    // Maximal safe growth rate; see CACM paper.
    // Here, we never more than double the reward.
    val growth_rate: BigDecimal = (1.0 / p_a).min(2.0)
    val reward = (growth_rate * current_reward).setScale(2, math.BigDecimal.RoundingMode.FLOOR)

    DebugLog(s"Timeout occurred. New reward is $$$reward because the estimated acceptance " +
      s"rate is $p_a per round and the current reward is $$$current_reward. Round = ${nextRound(tasks, suffered_timeout = true)}.",
      LogLevelDebug(),
      LogType.STRATEGY,
      question.id)

    reward
  }

  private def keepCurrentReward(tasks: List[Task]) : BigDecimal = {
    val current_reward = currentReward(tasks)

    DebugLog(s"Insufficient agreement. Keeping reward of $$$current_reward. Round = ${nextRound(tasks, suffered_timeout = false)}.",
      LogLevelDebug(),
      LogType.STRATEGY,
      question.id)

    current_reward
  }

  private def noResponsesReward(tasks: List[Task]) : BigDecimal = {
    // if timeouts occur, a round will contain mixed prices; take the max
    val current_reward = tasks.map(_.cost).max

    // double the reward
    val reward = (2.0 * current_reward).setScale(2, math.BigDecimal.RoundingMode.FLOOR)

    DebugLog(s"Timeout occurred. New reward is $$$reward because we cannot estimate acceptance " +
      s"rate and the current reward is $$$current_reward. Round = ${nextRound(tasks, suffered_timeout = true)}.",
      LogLevelDebug(),
      LogType.STRATEGY,
      question.id)

    reward
  }

  private def currentReward(tasks: List[Task]) : BigDecimal = {
    assert(tasks.nonEmpty)

    // if timeouts occur, a round will contain mixed prices; take the max
    tasks.map(_.cost).max
  }

  def calculateReward(tasks: List[Task], currentRound: Int, timeout_occurred: Boolean) : BigDecimal = {
    if (currentRound == 0 && tasks.isEmpty) {
      // first round, base case
      initialReward(tasks)
    } else if (numAnswered(tasks) != 0) {
      if (timeout_occurred) {
        // timeout case
        timeoutReward(tasks)
      } else {
        // non-timeout case
        keepCurrentReward(tasks)
      }
    } else {
      // the your-task-sucks-so-badly-nobody-will-take-it case
      noResponsesReward(tasks)
    }
  }
}
