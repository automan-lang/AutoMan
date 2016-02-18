package edu.umass.cs.automan.core.policy.aggregation

import java.util.UUID
import edu.umass.cs.automan.core.answer.{LowConfidenceAnswer, OverBudgetAnswer, Answer}
import edu.umass.cs.automan.core.logging.{LogType, LogLevelInfo, DebugLog}
import edu.umass.cs.automan.core.question.{Question, DiscreteScalarQuestion}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

object AdversarialPolicy {
  // this serializes computation of outcomes using MC, but it
  // ought to be outweighed by the fact that many tasks often
  // compute exactly the same thing
  var cache = Map[(Int,Int,Double,Int), Double]()
}

/**
  * This policy aggregates a set of discrete-valued responses
  * into a single best response using the adversarial scheme
  * outlined in the AutoMan OOPSLA '12 paper.
  * @param question The question
  */
class AdversarialPolicy(question: DiscreteScalarQuestion)
  extends ScalarPolicy(question) {
  protected[automan] val NumberOfSimulations = 1000000
  protected[automan] val PrecompPath = "/edu/umass/cs/automan/core/policy/aggregation/PossibilitiesTable.dat"

  val precompTable = PrecompTable.load(PrecompPath)

  DebugLog(s"Policy: adversarial; precomputed results ${if (precompTable.isDefined) "LOADED" else "NOT LOADED"}.",LogLevelInfo(),LogType.STRATEGY, question.id)

  /**
    * PRIVATE METHODS
    */

  /***
    * Calculates the best answer so far, returning the answer, cost, and confidence level.
    * @param tasks A list of tasks.
    * @return A tuple, (answer, cost, confidence)
    */
  private def answer_selector(tasks: List[Task]) : (Question#A,BigDecimal,Double) = {
    val bgrp = biggest_group(tasks)

    // find answer (actually an Option[Question#A]) of the largest group
    val answer_opt: Option[Question#A] = bgrp match { case (group,_) => group }

    // return the top result
    val value = answer_opt.get

    // get the confidence
    val conf = current_confidence(tasks)

    // calculate cost
    val cost = (bgrp match { case (_,ts) => ts.filterNot(_.from_memo) })
      .foldLeft(BigDecimal(0)){ case (acc,t) => acc + t.cost }

    (value, cost, conf)
  }

  /**
    * Find the most popular group.
    * @param tasks A list of tasks
    * @return A tuple representing the most popular answer
    *         (Optional since the largest answer may be 'no answer')
    *         and the set of tasks associated with that answer.
    */
  private def biggest_group(tasks: List[Task]) : (Option[Question#A], List[Task]) = {
    val rt = completed_workerunique_tasks(tasks)

    assert(rt.nonEmpty)

    // group by answer (which is actually an Option[Question#A] because Task.answer is Option[Question#A])
    val groups: Map[Option[Question#A], List[Task]] = rt.groupBy(_.answer)

    // find answer of the largest group
    groups.maxBy { case(group, ts) => ts.size }
  }

  /**
    * Calculate the number of additional unanimously-agreeing
    * answers needed to choose a single best answer with
    * high confidence.
    * @param trials The number of trials.
    * @param max_agr The number of agreeing answers for the most popular answer.
    * @param confidence The confidence threshold.
    * @return The number of tasks to schedule.
    */
  private def expected_for_agreement(trials: Int, max_agr: Int, confidence: Double) : Int = {
    // do the computation
    var to_run = 0
    var done = false
    while(!done) {
      val key = (numOpts, trials + to_run, confidence, 1000000)
      val min_required = AdversarialPolicy.synchronized {
        if (AdversarialPolicy.cache.contains(key)) {
          AdversarialPolicy.cache(key)
        } else {
          val min = AgreementSimulation.minToWin(numOpts, trials + to_run, confidence, 1000000)
          AdversarialPolicy.cache += key -> min
          min
        }
      }
      val expected = max_agr + to_run
      if (min_required < 0 || min_required > expected) {
        to_run += 1
      } else {
        done = true
      }
    }
    to_run
  }

  /**
    * Implementations
    */

  def current_confidence(tasks: List[Task]): Double = {
    val valid_ts = completed_workerunique_tasks(tasks)
    if (valid_ts.isEmpty) return 0.0 // bail if we have no valid responses
    val biggest_answer = valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
    AgreementSimulation.confidenceOfOutcome(numOpts, valid_ts.size, biggest_answer, NumberOfSimulations)
  }

  def is_confident(tasks: List[Task], num_hypotheses: Int): Boolean = {
    if (tasks.isEmpty) {
      DebugLog("Have no tasks; confidence is undefined.", LogLevelInfo(), LogType.STRATEGY, question.id)
      false
    } else {
      val conf = current_confidence(tasks)
      val thresh = bonferroni_confidence(question.confidence, num_hypotheses)
      if (conf >= thresh) {
        DebugLog("Reached or exceeded alpha = " + (1 - thresh).toString, LogLevelInfo(), LogType.STRATEGY, question.id)
        true
      } else {
        val valid_ts = completed_workerunique_tasks(tasks)
        if (valid_ts.nonEmpty) {
          val biggest_answer = valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
          DebugLog("Need more tasks for alpha = " + (1 - thresh) + "; have " + biggest_answer + " agreeing tasks.", LogLevelInfo(), LogType.STRATEGY, question.id)
        } else {
          DebugLog("Need more tasks for alpha = " + (1 - thresh) + "; currently have no agreement.", LogLevelInfo(), LogType.STRATEGY, question.id)
        }
        false
      }
    }
  }

  def max_agree(tasks: List[Task]) : Int = {
    val valid_ts = completed_workerunique_tasks(tasks)
    if (valid_ts.isEmpty) return 0
    valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
  }

  def numOpts : Int = {
    if(question.num_possibilities > BigInt(Int.MaxValue)) {
      Int.MaxValue
    } else {
      question.num_possibilities.toInt
    }
  }

  def num_to_run(tasks: List[Task], round: Int, reward: BigDecimal) : Int = {
    precompTable match {
      case Some(pt) =>
        if (round == 0) {
          // table is only valid for first round
          pt.getEntryOrNone(numOpts, reward) match {
            // there's an entry in the table
            case Some(ntr) => ntr
            // table has no entry
            case None => num_to_run_fallback(tasks, round, reward)
          }
        } else {
          // no table available; just compute results
          num_to_run_fallback(tasks, round, reward)
        }
      case None => num_to_run_fallback(tasks, round, reward)
    }
  }

  def num_to_run_fallback(tasks: List[Task], round: Int, reward: BigDecimal) : Int = {
    // eliminate duplicates from the list of Tasks
    val tasks_no_dupes = tasks.filter(_.state != SchedulerState.DUPLICATE)

    // the number of hypotheses is the current round number + 1, since we count from zero
    val adjusted_conf = bonferroni_confidence(question.confidence, round + 1)

    // compute # expected for agreement
    val expected = expected_for_agreement(tasks_no_dupes.size, max_agree(tasks_no_dupes), adjusted_conf)

    val biggest_bang =
      math.min(
        math.floor(question.budget.toDouble/reward.toDouble),
        math.floor(question.time_value_per_hour.toDouble/question.wage.toDouble)
      )

    math.max(expected, biggest_bang).toInt
  }

  def pessimism() = {
    val p: Double = math.max((question.time_value_per_hour/question.wage).toDouble, 1.0)
    if (p > 1) {
      DebugLog("Using pessimistic (expensive) strategy.", LogLevelInfo(), LogType.STRATEGY, question.id)
    } else {
      DebugLog("Using Using optimistic (cheap) strategy.", LogLevelInfo(), LogType.STRATEGY, question.id)
    }
    p
  }

  def rejection_response(tasks: List[Task]): String = {
    if (tasks.isEmpty) {
      "Your answer is incorrect.  " +
        "We value your feedback, so if you think that we are in error, please contact us."
    } else {
      tasks.head.answer match {
        case Some(a) =>
          "Your answer is incorrect.  The correct answer is '" + a + "'.  " + "" +
            "We value your feedback, so if you think that we are in error, please contact us."
        case None =>
          "Your answer is incorrect.  " +
            "We value your feedback, so if you think that we are in error, please contact us."
      }
    }
  }

  def select_answer(tasks: List[Task]) : Question#AA = {
    answer_selector(tasks) match { case (value,cost,conf) =>
      DebugLog("Most popular answer is " + value.toString, LogLevelInfo(), LogType.STRATEGY, question.id)
      Answer(value, cost, conf).asInstanceOf[Question#AA]
    }
  }

  def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal) : Question#AA = {
    // if we've never scheduled anything,
    // there will be no largest group
    if(completed_workerunique_tasks(tasks).isEmpty) {
      OverBudgetAnswer(need, have).asInstanceOf[Question#AA]
    } else {
      answer_selector(tasks) match {
        case (value, cost, conf) =>
          DebugLog("Over budget.  Best answer so far is " + value.toString, LogLevelInfo(), LogType.STRATEGY, question.id)
          LowConfidenceAnswer(value, cost, conf).asInstanceOf[Question#AA]
      }
    }
  }

  def spawn(tasks: List[Task], had_timeout: Boolean): List[Task] = {
    // determine current round
    val round = if (tasks.nonEmpty) {
      tasks.map(_.round).max
    } else { 0 }

    var nextRound = round

    // determine duration
    val worker_timeout_in_s = question._timeout_policy_instance.calculateWorkerTimeout(tasks, round, had_timeout)
    val task_timeout_in_s = question._timeout_policy_instance.calculateTaskTimeout(worker_timeout_in_s)

    // determine reward
    val reward = question._price_policy_instance.calculateReward(tasks, round, had_timeout)

    // num to spawn
    val num_to_spawn = if (had_timeout) {
      tasks.count { t => t.round == round && t.state == SchedulerState.TIMEOUT }
    } else {
      // (don't spawn more if any are running)
      if (tasks.count(_.state == SchedulerState.RUNNING) == 0) {
        // whenever we need to run MORE, we update the round counter
        nextRound = round + 1
        num_to_run(tasks, round, reward)
      } else {
        return List[Task]() // Be patient!
      }
    }

    DebugLog("You should spawn " + num_to_spawn +
      " more Tasks at $" + reward + "/task, " +
      task_timeout_in_s + "s until question timeout, " +
      worker_timeout_in_s + "s until worker task timeout.", LogLevelInfo(), LogType.STRATEGY,
      question.id)

    // allocate Task objects
    val new_tasks = (0 until num_to_spawn).map { i =>
      val now = new java.util.Date()
      val t = new Task(
        UUID.randomUUID(),
        question,
        nextRound,
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
      t
    }.toList

    new_tasks
  }

  def tasks_to_accept(tasks: List[Task]): List[Task] = {
    (biggest_group(tasks) match { case (_, ts) => ts })
      .filter(not_final)
  }

  def tasks_to_reject(tasks: List[Task]): List[Task] = {
    val accepts = tasks_to_accept(tasks).toSet
    tasks.filter { t =>
      !accepts.contains(t) &&
        not_final(t)
    }
  }
}