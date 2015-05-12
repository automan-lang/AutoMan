package edu.umass.cs.automan.core.strategy

import java.util.UUID

import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.DistributionQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

class DefaultDistributionStrategy(question: DistributionQuestion)
  extends DistributionValidationStrategy(question) {

  def num_to_run(tasks: List[Task]) : Int = {
    // additional number needed to reach num_samples
    Math.max(question.sample_size - outstanding_tasks(tasks).size, 0)
  }

  override def spawn(tasks: List[Task], round: Int, suffered_timeout: Boolean): List[Task] = {
    // this will not wait for the end of a round to spawn new tasks
    // (although the scheduler may)
    val num_to_spawn = if (outstanding_tasks(tasks).size < question.sample_size) {
      num_to_run(tasks)
    } else {
      return List[Task]() // Be patient!
    }

    DebugLog("You should spawn " + num_to_spawn +
      " more Tasks at $" + question.reward + "/task, " +
      question.question_timeout_in_s + "s until question timeout, " +
      question.worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY, question.id)

    // allocate Task objects
    val new_tasks = (0 until num_to_spawn).map { i =>
      val now = new java.util.Date()
      val t = new Task(
        UUID.randomUUID(),
        question,
        question.question_timeout_in_s,
        question.worker_timeout_in_s,
        question.reward,
        now,
        SchedulerState.READY,
        from_memo = false,
        None,
        None,
        now
      )
      DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY,question.id)
      t
    }.toList

    new_tasks
  }
}