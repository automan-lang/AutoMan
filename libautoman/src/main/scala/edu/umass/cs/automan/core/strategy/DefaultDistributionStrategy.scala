package edu.umass.cs.automan.core.strategy

import java.util.UUID

import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.DistributionQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

class DefaultDistributionStrategy(question: DistributionQuestion)
  extends DistributionValidationStrategy(question) {

  def num_to_run(thunks: List[Thunk]) : Int = {
    val np: Int = if(question.num_possibilities > BigInt(Int.MaxValue)) 1000 else question.num_possibilities.toInt

    // update # of unique workers
    val num_unique_workers = completed_thunks(thunks).map { t => t.worker_id }.distinct.size

    // additional number needed to reach num_samples
    Math.max(question.sample_size - outstanding_thunks(thunks).size, 0)
  }

  override def spawn(thunks: List[Thunk], suffered_timeout: Boolean): List[Thunk] = {
    // this will not wait for the end of a round to spawn new tasks
    // (although the scheduler may)
    val num_to_spawn = if (outstanding_thunks(thunks).size < question.sample_size) {
      num_to_run(thunks)
    } else {
      return List[Thunk]() // Be patient!
    }

    DebugLog("You should spawn " + num_to_spawn +
      " more Thunks at $" + question.reward + "/thunk, " +
      question.question_timeout_in_s + "s until question timeout, " +
      question.worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY, question.id)

    // allocate Thunk objects
    val new_thunks = (0 until num_to_spawn).map { i =>
      val now = new java.util.Date()
      val t = new Thunk(
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

    new_thunks
  }
}