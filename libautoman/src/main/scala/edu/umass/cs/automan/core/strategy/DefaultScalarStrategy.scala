package edu.umass.cs.automan.core.strategy

import java.util

import edu.umass.cs.automan.core.answer.ScalarAnswer
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.question.{ScalarQuestion, CheckboxQuestion}
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities}

object DefaultScalarStrategy {
  val table = new util.HashMap[(Int,Int,Int,Double),Int]()
}

class DefaultScalarStrategy[Q <: ScalarQuestion, A <: ScalarAnswer](question: Q) extends ScalarValidationStrategy[Q,A](question) {
  Utilities.DebugLog("DEFAULTSCALAR strategy loaded!",LogLevel.INFO,LogType.STRATEGY,_computation_id)

  def current_confidence: Double = {
    val valid_ts = _thunks.filter(t => t.state == SchedulerState.RETRIEVED || t.state == SchedulerState.PROCESSED )
    if (valid_ts.size == 0) return 0.0 // bail if we have no valid responses
    val biggest_answer = valid_ts.groupBy(_.answer.comparator).maxBy{ case(sym,ts) => ts.size }._2.size
    MonteCarlo.confidenceOfOutcome(_num_possibilities.toInt, _thunks.size, biggest_answer, 1000000)
  }
  def is_confident: Boolean = {
    if (_thunks.size == 0) {
      Utilities.DebugLog("Have no thunks; confidence is undefined.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
      false
    } else {
      val valid_ts = _thunks.filter(t => t.state == SchedulerState.RETRIEVED || t.state == SchedulerState.PROCESSED )
      if (valid_ts.size == 0) return false // bail if we have no valid responses
      val biggest_answer = valid_ts.groupBy(_.answer.comparator).maxBy{ case(sym,ts) => ts.size }._2.size

      // TODO: MonteCarlo simulator needs to take BigInts!
      val min_agree = MonteCarlo.requiredForAgreement(_num_possibilities.toInt, _thunks.size, _confidence, 1000000)
      if (biggest_answer >= min_agree) {
        Utilities.DebugLog("Reached or exceeded alpha = " + (1 - _confidence).toString, LogLevel.INFO, LogType.STRATEGY, _computation_id)
        true
      } else {
        Utilities.DebugLog("Need " + min_agree + " for alpha = " + (1 - _confidence) + "; have " + biggest_answer, LogLevel.INFO, LogType.STRATEGY, _computation_id)
        false
      }
    }
  }
  def max_agree: Int = {
    val valid_ts = _thunks.filter(t => t.state == SchedulerState.RETRIEVED || t.state == SchedulerState.PROCESSED )
    if (valid_ts.size == 0) return 0
    valid_ts.groupBy(_.answer.comparator).maxBy{ case(sym,ts) => ts.size }._2.size
  }
  def spawn(had_timeout: Boolean): List[Thunk[A]] = {
    // num to spawn
    val num_to_spawn = if (_thunks.filter(_.state == SchedulerState.RUNNING).size == 0) {
      num_to_run(question)
    } else {
      return List[Thunk[A]]() // Be patient!
    }

    // determine duration
    if (had_timeout) {
      Utilities.DebugLog("Had a timeout; doubling worker timeout.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
      question.worker_timeout_in_s *= 2
    }

    Utilities.DebugLog("You should spawn " + num_to_spawn +
                        " more Thunks at $" + question.reward + "/thunk, " +
                          question.question_timeout_in_s + "s until question timeout, " +
                          question.worker_timeout_in_s + "s until worker task timeout.", LogLevel.INFO, LogType.STRATEGY,
                        _computation_id)

    // allocate Thunk objects
    val new_thunks = (0 until num_to_spawn).map { i =>
      _budget_committed += question.reward
      if (_budget_committed > question.budget) {
        Utilities.DebugLog("Over budget. budget_committed = " + _budget_committed + " > budget = " + question.budget, LogLevel.FATAL, LogType.STRATEGY, _computation_id)
        throw OverBudgetException("")
      }
      val t = new Thunk[A](question, question.question_timeout_in_s, question.worker_timeout_in_s, question.reward, _computation_id)
      Utilities.DebugLog("spawned question_id = " + question.id_string,LogLevel.INFO,LogType.STRATEGY,_computation_id)
      t
    }.toList
    _thunks = new_thunks ::: _thunks

    // mark some of them as duals if the question is a CheckboxQuestion
    question match {
      case cbq: CheckboxQuestion => (0 until (new_thunks.size / 2)).foreach{ i => new_thunks(i).is_dual = true }
      case _ => {}
    }
    
    new_thunks
  }

  def num_to_run(q: Q) : Int = {
    val np: Int = if(q.num_possibilities > BigInt(Int.MaxValue)) 1000 else q.num_possibilities.toInt

    math.max(expected_for_agreement(np, _thunks.size, max_agree, q.confidence).toDouble,
             math.min(math.floor(q.budget.toDouble/q.reward.toDouble),
                      math.floor(q.time_value_per_hour.toDouble/q.wage.toDouble)
             )
    ).toInt
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

  def choose_starting_n(question: Q) : Int = {
    // at start, we assume that all workers will agree unanimously
    var duplicates_required = 1

    // formula is:
    // (# of ways to have unanimous answer) * (probability of a given choice)^(trials)
    while (question.num_possibilities.toDouble * math.pow(1.0/question.num_possibilities.toDouble, duplicates_required) > (1.0 - confidence)) {
      duplicates_required += 1
    }

    // multiply by pessimism factor
    (duplicates_required * pessimism(question)).toInt
  }

  def pessimism(q: Q) = {
    val p: Double = math.max((q.time_value_per_hour/q.wage).toDouble, 1.0)
    if (p > 1) {
      Utilities.DebugLog("Using pessimistic (expensive) strategy.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
    } else {
      Utilities.DebugLog("Using Using optimistic (cheap) strategy.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
    }
    p
  }
}