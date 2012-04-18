package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.question.{CheckboxQuestion, Question}
import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.Utilities

class DefaultStrategy extends ValidationStrategy {
  Utilities.DebugLog("DEFAULT strategy loaded!",0,"STRATEGY")

  def current_confidence: Double = {
    val valid_ts = _thunks.filter(t => t.state == SchedulerState.RETRIEVED || t.state == SchedulerState.PROCESSED )
    if (valid_ts.size == 0) return 0.0 // bail if we have no valid responses
    val biggest_answer = valid_ts.groupBy(_.answer.comparator).maxBy{ case(sym,ts) => ts.size }._2.size
    MonteCarlo.confidenceOfOutcome(_num_possibilities.toInt, _thunks.size, biggest_answer, 1000000)
  }
  def is_confident: Boolean = {
    if (_thunks.size == 0) {
      Utilities.DebugLog("Have no thunks; confidence is undefined.", 0, "STRATEGY")
      false
    } else {
      val valid_ts = _thunks.filter(t => t.state == SchedulerState.RETRIEVED || t.state == SchedulerState.PROCESSED )
      if (valid_ts.size == 0) return false // bail if we have no valid responses
      val biggest_answer = valid_ts.groupBy(_.answer.comparator).maxBy{ case(sym,ts) => ts.size }._2.size

      // TODO: MonteCarlo simulator needs to take BigInts!
      val min_agree = MonteCarlo.requiredForAgreement(_num_possibilities.toInt, _thunks.size, _confidence, 1000000)
      if (biggest_answer >= min_agree) {
        Utilities.DebugLog("DEBUG: STRATEGY: Reached or exceeded alpha = " + (1 - _confidence).toString, 0, "STRATEGY")
        true
      } else {
        Utilities.DebugLog("DEBUG: STRATEGY: Need " + min_agree + " for alpha = " + (1 - _confidence) + "; have " + biggest_answer, 0, "STRATEGY")
        false
      }
    }
  }
  def max_agree: Int = {
    val valid_ts = _thunks.filter(t => t.state == SchedulerState.RETRIEVED || t.state == SchedulerState.PROCESSED )
    if (valid_ts.size == 0) return 0
    valid_ts.groupBy(_.answer.comparator).maxBy{ case(sym,ts) => ts.size }._2.size
  }
  def spawn(question: Question, had_timeout: Boolean): List[Thunk] = {
    // num to spawn
    val num_to_spawn = if (_thunks.filter(_.state == SchedulerState.RUNNING).size == 0) {
      num_to_run(question)
    } else {
      return List[Thunk]() // Be patient!
    }

    // determine duration
    if (had_timeout) {
      Utilities.DebugLog("Had a timeout; doubling worker timeout.", 0, "STRATEGY")
      question.worker_timeout_in_s *= 2
    }

    Utilities.DebugLog("You should spawn " + num_to_spawn +
                        " more Thunks at $" + question.reward + "/thunk, " +
                          question.question_timeout_in_s + "s until question timeout, " +
                          question.worker_timeout_in_s + "s until worker task timeout.", 0, "STRATEGY")

    // allocate Thunk objects
    val new_thunks = (0 until num_to_spawn).map { i =>
      _budget_committed += question.reward
      if (_budget_committed > question.budget) {
        Utilities.DebugLog("Over budget. budget_committed = " + _budget_committed + " > budget = " + question.budget, 0, "STRATEGY")
        throw OverBudgetException("")
      }
      new Thunk(question, question.question_timeout_in_s, question.worker_timeout_in_s, question.reward)
    }.toList
    _thunks = new_thunks ::: _thunks

    // mark some of them as duals if the question is a CheckboxQuestion
    question match {
      case cbq: CheckboxQuestion[_] => (0 until (new_thunks.size / 2)).foreach{ i => new_thunks(i).is_dual = true }
      case _ => {}
    }
    
    new_thunks
  }

  def num_to_run(q: Question) : Int = {
    val np: Int = if(q.num_possibilities > BigInt(Int.MaxValue)) 1000 else q.num_possibilities.toInt

    math.max(expected_for_agreement(np, _thunks.size, max_agree, q.confidence).toDouble,
             math.min(math.floor(q.budget.toDouble/q.reward.toDouble),
                      math.floor(q.time_value_per_hour.toDouble/q.wage.toDouble)
             )
    ).toInt
  }
  
  def expected_for_agreement(num_possibilities: Int, trials: Int,  max_agr: Int, confidence: Double) : Int = {
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
    to_run
  }

  def choose_starting_n(question: Question) : Int = {
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

  def pessimism(q: Question) = {
    val p: Double = math.max((q.time_value_per_hour/q.wage).toDouble, 1.0)
    if (p > 1) {
      Utilities.DebugLog("Using pessimistic (expensive) strategy.", 0, "STRATEGY")
    } else {
      Utilities.DebugLog("Using Using optimistic (cheap) strategy.", 0, "STRATEGY")
    }
    p
  }
}