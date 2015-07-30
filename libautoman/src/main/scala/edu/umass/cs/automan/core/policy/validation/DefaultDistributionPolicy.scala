package edu.umass.cs.automan.core.policy.validation

import java.util.UUID

import edu.umass.cs.automan.core.logging.{DebugLog, LogLevel, LogType}
import edu.umass.cs.automan.core.question.DistributionQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import edu.umass.cs.automan.core.policy.price.FixedPricePolicy

class DefaultDistributionPolicy(question: DistributionQuestion)
  extends DistributionValidationPolicy(question) {

  def num_to_run(tasks: List[Task]) : Int = {
    // additional number needed to reach num_samples
    Math.max(question.sample_size - outstanding_tasks(tasks).size, 0)
  }

  def spawn(tasks: List[Task], had_timeout: Boolean): List[Task] = {
    // determine current round
    val round = if (tasks.nonEmpty) { tasks.map(_.round).max } else { 1 }

    // num to spawn (don't spawn more if any are running)
    val num_to_spawn = if (tasks.count(_.state == SchedulerState.RUNNING) == 0) {
      num_to_run(tasks)
    } else {
      return List[Task]() // Be patient!
    }

    // determine duration
    val worker_timeout_in_s = question._timeout_policy_instance.calculateWorkerTimeout(tasks, round)
    val task_timeout_in_s = question._timeout_policy_instance.calculateTaskTimeout(worker_timeout_in_s)

    // determine reward
    val reward = question._price_policy_instance.calculateReward(tasks, round, had_timeout)

    DebugLog("You should spawn " + num_to_spawn +
      " more Tasks at $" + reward + "/task, " +
      task_timeout_in_s + "s until question timeout, " +
      worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY,
      question.id)

    // allocate Task objects
    val new_tasks = (0 until num_to_spawn).map { i =>
      val now = new java.util.Date()
      val t = new Task(
        UUID.randomUUID(),
        question,
        round,
        task_timeout_in_s,
        worker_timeout_in_s,
        reward,
        now,
        SchedulerState.READY,
        from_memo = false,
        None,
        None,
        now
      )
      DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY, question.id)
      t
    }.toList

    new_tasks
  }
}