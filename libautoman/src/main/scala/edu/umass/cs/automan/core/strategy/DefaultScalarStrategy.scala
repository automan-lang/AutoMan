package edu.umass.cs.automan.core.strategy

import java.util.UUID
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question.{Question, ScalarQuestion}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

class DefaultScalarStrategy(question: ScalarQuestion)
  extends ScalarValidationStrategy(question) {
  DebugLog("DEFAULTSCALAR strategy loaded.",LogLevel.INFO,LogType.STRATEGY, question.id)

  def bonferroni_confidence(confidence: Double, rounds: Int) : Double = {
    1 - (1 - confidence) / rounds.toDouble
  }
  def current_confidence(thunks: List[Thunk]): Double = {
    val valid_ts = completed_workerunique_thunks(thunks)
    if (valid_ts.size == 0) return 0.0 // bail if we have no valid responses
    val biggest_answer = valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
    val conf = 1.0 - MonteCarlo.CalculateProbability(question.num_possibilities.toInt, thunks.size, biggest_answer, MonteCarlo.NumberOfSimulations)
    conf
  }
  def is_confident(thunks: List[Thunk], round: Int): Boolean = {
    if (thunks.size == 0) {
      DebugLog("Have no thunks; confidence is undefined.", LogLevel.INFO, LogType.STRATEGY, question.id)
      false
    } else {
      val conf = current_confidence(thunks)
      val thresh = bonferroni_confidence(question.confidence, round)
      if (conf >= thresh) {
        DebugLog("Reached or exceeded alpha = " + (1 - thresh).toString, LogLevel.INFO, LogType.STRATEGY, question.id)
        true
      } else {
        val valid_ts = completed_workerunique_thunks(thunks)
        val biggest_answer = valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
        DebugLog("Need more tasks for alpha = " + (1 - thresh) + "; have " + biggest_answer, LogLevel.INFO, LogType.STRATEGY, question.id)
        false
      }
    }
  }
  def max_agree(thunks: List[Thunk]) : Int = {
    val valid_ts = completed_workerunique_thunks(thunks)
    if (valid_ts.size == 0) return 0
    valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
  }
  def spawn(thunks: List[Thunk], round: Int, had_timeout: Boolean): List[Thunk] = {
    // num to spawn (don't spawn more if any are running)
    val num_to_spawn = if (thunks.count(_.state == SchedulerState.RUNNING) == 0) {
      num_to_run(thunks, round)
    } else {
      return List[Thunk]() // Be patient!
    }

    // determine duration
    if (had_timeout) {
      DebugLog("Had a timeout; doubling worker timeout.", LogLevel.INFO, LogType.STRATEGY, question.id)
      question.worker_timeout_in_s *= 2
    }

    DebugLog("You should spawn " + num_to_spawn +
                        " more Thunks at $" + question.reward + "/thunk, " +
                          question.question_timeout_in_s + "s until question timeout, " +
                          question.worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY,
                          question.id)

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
      DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY, question.id)
      t
    }.toList

    new_thunks
  }

  def num_to_run(thunks: List[Thunk], round: Int) : Int = {
    // eliminate duplicates from the list of Thunks
    val thunks_no_dupes = thunks.filter(_.state != SchedulerState.DUPLICATE)

    val options: Int = if(question.num_possibilities > BigInt(Int.MaxValue)) 1000 else question.num_possibilities.toInt

    val adjusted_conf = bonferroni_confidence(question.confidence, round)

    val expected = MonteCarlo.HowManyMoreTrials(thunks_no_dupes.size, max_agree(thunks_no_dupes), options, adjusted_conf)
    val biggest_bang =
      math.min(
        math.floor(question.budget.toDouble/question.reward.toDouble),
        math.floor(question.time_value_per_hour.toDouble/question.wage.toDouble)
      )

    math.max( expected, biggest_bang ).toInt
  }

  def pessimism() = {
    val p: Double = math.max((question.time_value_per_hour/question.wage).toDouble, 1.0)
    if (p > 1) {
      DebugLog("Using pessimistic (expensive) strategy.", LogLevel.INFO, LogType.STRATEGY, question.id)
    } else {
      DebugLog("Using Using optimistic (cheap) strategy.", LogLevel.INFO, LogType.STRATEGY, question.id)
    }
    p
  }
}