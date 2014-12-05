package edu.umass.cs.automan.core.strategy

import java.util.UUID

import edu.umass.cs.automan.core.answer.ScalarAnswer
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}
import edu.umass.cs.automan.core.question.DistributionQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

class DefaultDistributionStrategy[Q <: DistributionQuestion, A <: ScalarAnswer, B](question: Q, num_samples: Int = 30)
  extends DistributionValidationStrategy[Q,A,B](question) {

  def num_to_run(q: Q) : Int = {
    val np: Int = if(q.num_possibilities > BigInt(Int.MaxValue)) 1000 else q.num_possibilities.toInt

    // update # of unique workers
    val unique_workers = completed_thunks.map { t => t.worker_id }.distinct.size
    ValidationStrategy.overwrite(q.title, q.text, _computation_id, unique_workers, completed_thunks.size)

    // additional number needed to reach num_samples
    val n = Math.max(num_samples - outstanding_thunks.size, 0)

    // if we aren't using disqualifications, calculate the expected number of
    // worker reparticipations and inflate n accordingly
    ValidationStrategy.work_uniqueness(q.title, q.text) match {
      case Some(u) =>
        if (q.use_disqualifications) {
          n.toInt
        } else {
          Math.ceil(n / u.toDouble).toInt
        }
      case None => n.toInt
    }
  }

  // the distribution strategy only adjusts the numbers of thunks;
  // it never adjusts timeouts or rewards
  def spawn(had_timeout: Boolean): List[Thunk[A]] = {
    // this will not wait for the end of a round to spawn new tasks
    // (although the scheduler may)
    val num_to_spawn = if (outstanding_thunks.size < num_samples) {
      num_to_run(question)
    } else {
      return List[Thunk[A]]() // Be patient!
    }

    Utilities.DebugLog("You should spawn " + num_to_spawn +
      " more Thunks at $" + question.reward + "/thunk, " +
      question.question_timeout_in_s + "s until question timeout, " +
      question.worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY,
      _computation_id)

    // allocate Thunk objects
    val new_thunks = (0 until num_to_spawn).map { i =>
      val t = new Thunk[A](UUID.randomUUID(),
                           question,
                           question.question_timeout_in_s,
                           question.worker_timeout_in_s,
                           question.reward,
                           _computation_id,
                           new java.util.Date(),
                           SchedulerState.READY,
                           false,
                           None,
                           None
                          )
      Utilities.DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY,_computation_id)
      t
    }.toList

    // reserve money for them
    pay_for_thunks(new_thunks)

    _thunks = new_thunks ::: _thunks

    new_thunks
  }
}
