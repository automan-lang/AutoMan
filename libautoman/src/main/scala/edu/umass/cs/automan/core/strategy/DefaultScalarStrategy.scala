package edu.umass.cs.automan.core.strategy

import java.util
import java.util.UUID

import edu.umass.cs.automan.core.question.ScalarQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities}

import scala.collection.immutable.Iterable

object DefaultScalarStrategy {
  val table = new util.HashMap[(Int,Int,Int,Double),Int]()
}

class DefaultScalarStrategy[A](question: ScalarQuestion[A])
  extends ScalarValidationStrategy[A](question) {
  Utilities.DebugLog("DEFAULTSCALAR strategy loaded.",LogLevel.INFO,LogType.STRATEGY, question.id)

  def current_confidence(thunks: List[Thunk[A]]): Double = {
    val valid_ts = completed_workerunique_thunks(thunks)
    if (valid_ts.size == 0) return 0.0 // bail if we have no valid responses
    val biggest_answer = valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
    MonteCarlo.confidenceOfOutcome(_num_possibilities.toInt, thunks.size, biggest_answer, 1000000)
  }
  def is_confident(thunks: List[Thunk[A]]): Boolean = {
    if (thunks.size == 0) {
      Utilities.DebugLog("Have no thunks; confidence is undefined.", LogLevel.INFO, LogType.STRATEGY, question.id)
      false
    } else {
      val valid_ts = completed_workerunique_thunks(thunks)
      if (valid_ts.size == 0) return false // bail if we have no valid responses
      val biggest_answer = valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size

      // TODO: MonteCarlo simulator needs to take BigInts!
      val min_agree = MonteCarlo.requiredForAgreement(_num_possibilities.toInt, thunks.size, _confidence, 1000000)
      if (biggest_answer >= min_agree) {
        Utilities.DebugLog("Reached or exceeded alpha = " + (1 - question.id).toString, LogLevel.INFO, LogType.STRATEGY, question.id)
        true
      } else {
        Utilities.DebugLog("Need " + min_agree + " for alpha = " + (1 - _confidence) + "; have " + biggest_answer, LogLevel.INFO, LogType.STRATEGY, question.id)
        false
      }
    }
  }
  def max_agree(thunks: List[Thunk[A]]) : Int = {
    val valid_ts = completed_workerunique_thunks(thunks)
    if (valid_ts.size == 0) return 0
    valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
  }
  def spawn(thunks: List[Thunk[A]], had_timeout: Boolean): List[Thunk[A]] = {
    // num to spawn (don't spawn more if any are running)
    val num_to_spawn = if (thunks.count(_.state == SchedulerState.RUNNING) == 0) {
      num_to_run(thunks)
    } else {
      return List[Thunk[A]]() // Be patient!
    }

    // determine duration
    if (had_timeout) {
      Utilities.DebugLog("Had a timeout; doubling worker timeout.", LogLevel.INFO, LogType.STRATEGY, question.id)
      question.worker_timeout_in_s *= 2
    }

    Utilities.DebugLog("You should spawn " + num_to_spawn +
                        " more Thunks at $" + question.reward + "/thunk, " +
                          question.question_timeout_in_s + "s until question timeout, " +
                          question.worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY,
                          question.id)

    // allocate Thunk objects
    val new_thunks = (0 until num_to_spawn).map { i =>
      val t = new Thunk[A](UUID.randomUUID(),
        question,
        question.question_timeout_in_s,
        question.worker_timeout_in_s,
        question.reward,
        new java.util.Date(),
        SchedulerState.READY,
        false
      )
      Utilities.DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY, question.id)
      t
    }.toList

    new_thunks
  }

  def num_to_run(thunks: List[Thunk[A]]) : Int = {
    val np: Int = if(question.num_possibilities > BigInt(Int.MaxValue)) 1000 else question.num_possibilities.toInt

    // update # of unique workers
    val unique_workers = completed_thunks(thunks).map { t => t.worker_id }.distinct.size
    ValidationStrategy.overwrite(question.title, question.text, _computation_id, unique_workers, completed_thunks(thunks).size)

    // number needed for agreement, adjusted for programmer time-value
    val n = math.max(expected_for_agreement(np, thunks.size, max_agree(thunks), question.confidence).toDouble,
             math.min(math.floor(question.budget.toDouble/question.reward.toDouble),
                      math.floor(question.time_value_per_hour.toDouble/question.wage.toDouble)
             )
    )

    // if we aren't using disqualifications, calculate the expected number of
    // worker reparticipations and inflate n accordingly
    ValidationStrategy.work_uniqueness(question.title, question.text) match {
      case Some(u) =>
        if (question.use_disqualifications) {
          n.toInt
        } else {
          Math.ceil(n / u.toDouble).toInt
        }
      case None => n.toInt
    }
  }
  
  def expected_for_agreement(num_possibilities: Int, trials: Int,  max_agr: Int, confidence: Double) : Int = {
    DefaultScalarStrategy.table.synchronized {
      // check table
      if (!DefaultScalarStrategy.table.containsKey((num_possibilities, trials, max_agr, confidence))) {
        // do the computation
        var to_run = 0
        var done = false
        while(!done) {
          val min_required = MonteCarlo.requiredForAgreement(num_possibilities, trials + to_run, confidence, 1000000)
          val expected = max_agr + to_run
          if (min_required < 0 || min_required > expected) {
            to_run += 1
          } else {
            done = true
          }
        }

        // insert into table
        DefaultScalarStrategy.table.put((num_possibilities, trials, max_agr, confidence), to_run)

        to_run
      } else {
        DefaultScalarStrategy.table.get((num_possibilities, trials, max_agr, confidence))
      }
    }
  }

  def choose_starting_n() : Int = {
    // at start, we assume that all workers will agree unanimously
    var duplicates_required = 1

    // formula is:
    // (# of ways to have unanimous answer) * (probability of a given choice)^(trials)
    while (question.num_possibilities.toDouble * math.pow(1.0/question.num_possibilities.toDouble, duplicates_required) > (1.0 - confidence)) {
      duplicates_required += 1
    }

    // multiply by pessimism factor
    (duplicates_required * pessimism()).toInt
  }

  def pessimism() = {
    val p: Double = math.max((question.time_value_per_hour/question.wage).toDouble, 1.0)
    if (p > 1) {
      Utilities.DebugLog("Using pessimistic (expensive) strategy.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
    } else {
      Utilities.DebugLog("Using Using optimistic (cheap) strategy.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
    }
    p
  }
}