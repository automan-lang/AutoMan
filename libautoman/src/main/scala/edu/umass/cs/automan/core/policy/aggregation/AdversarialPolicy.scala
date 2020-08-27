package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.answer.{LowConfidenceAnswer, OverBudgetAnswer, Answer}
import edu.umass.cs.automan.core.logging.{LogType, LogLevelInfo, DebugLog}
import edu.umass.cs.automan.core.question.{Response, Question, DiscreteScalarQuestion}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

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
    * @param num_comparisons The number of times we called is_done.
    * @return A tuple, (answer, cost, confidence)
    */
  private def answer_selector(tasks: List[Task], num_comparisons: Int) : (Question#A,BigDecimal,Double,Array[Response[Question#A]]) = {
    val valid_tasks = completed_workerunique_tasks(tasks)

    val bgrp = biggest_group(valid_tasks)

    // find answer (actually an Option[Question#A]) of the largest group
    val answer_opt: Option[Question#A] = bgrp match { case (group,_) => group }

    // return the top result
    val value = answer_opt.get

    // get the confidence
    val conf = current_confidence(valid_tasks)

    // calculate cost
    val cost = (bgrp match { case (_,ts) => ts.filterNot(_.from_memo) })
      .foldLeft(BigDecimal(0)){ case (acc,t) => acc + t.cost }

    // distribution
    val dist = getDistribution(tasks)

    (value, cost, conf, dist)
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
    var to_run =
      if (trials == 0) {
        DebugLog("Running a minimum of 2 trials.", LogLevelInfo(), LogType.STRATEGY, question.id)
        2
      } else {
        trials
      }
    var done = false
    while(!done) {
      val min_required = AgreementSimulation.minToWin(numOpts, trials + to_run, confidence, 1000000, question.id)
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

  def is_confident(tasks: List[Task], num_comparisons: Int): (Boolean,Int) = {
    if (tasks.isEmpty) {
      DebugLog("Have no tasks; confidence is undefined.", LogLevelInfo(), LogType.STRATEGY, question.id)
      (false, num_comparisons)
    } else {
      val conf = current_confidence(tasks)
      val thresh = bonferroni_confidence(question.confidence, num_comparisons)
      val done = if (conf >= thresh) {
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
      (done, num_comparisons + 1)
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

  def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal) : Int = {
    precompTable match {
      case Some(pt) =>
        if (num_comparisons == 1) {
          // table is only valid for first round
          pt.getEntryOrNone(numOpts, reward) match {
            // there's an entry in the table
            case Some(ntr) => ntr
            // table has no entry
            case None => num_to_run_fallback(tasks, num_comparisons, reward)
          }
        } else {
          // no table available; just compute results
          num_to_run_fallback(tasks, num_comparisons, reward)
        }
      case None => num_to_run_fallback(tasks, num_comparisons, reward)
    }
  }

  def num_to_run_fallback(tasks: List[Task], num_comparisons: Int, reward: BigDecimal) : Int = {
    // eliminate duplicates from the list of Tasks
    val tasks_no_dupes = tasks.filter(_.state != SchedulerState.DUPLICATE)

    // using the number of comparisons, adjust our confidence level
    val adjusted_conf = bonferroni_confidence(question.confidence, num_comparisons)

    // Compute # expected for agreement.
    // Rationale:
    //   This is a multinomial experiment.  Each task is a trial (for the two-choice case, a Bernoulli trial).
    //   The experiment is repeated until random agreement is unlikely given the supplied confidence level.
    //   We add the result of this trial to the number of tasks that already agree.
    val expected = expected_for_agreement(trials = tasks_no_dupes.size,
                                          max_agr = max_agree(tasks_no_dupes),
                                          confidence = adjusted_conf)

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

  def select_answer(tasks: List[Task], num_comparisons: Int) : Question#AA = {
    answer_selector(tasks, num_comparisons) match { case (value,cost,conf,dist) =>
      DebugLog("Most popular answer is " + value.toString, LogLevelInfo(), LogType.STRATEGY, question.id)
      Answer(value, cost, conf, question, dist).asInstanceOf[Question#AA]
    }
  }

  def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal, num_comparisons: Int) : Question#AA = {
    // if we've never scheduled anything,
    // there will be no largest group
    if(completed_workerunique_tasks(tasks).isEmpty) {
      OverBudgetAnswer(need, have, question).asInstanceOf[Question#AA]
    } else {
      answer_selector(tasks, num_comparisons) match {
        case (value, cost, conf, dist) =>
          DebugLog("Over budget.  Best answer so far is " + value.toString, LogLevelInfo(), LogType.STRATEGY, question.id)
          LowConfidenceAnswer(value, cost, conf, question, dist).asInstanceOf[Question#AA]
      }
    }
  }

  def tasks_to_accept(tasks: List[Task]): List[Task] = {
    val cancels = tasks_to_cancel(tasks).toSet
    (biggest_group(tasks) match { case (_, ts) => ts })
      .filter { t =>
        not_final(t) &&
        !cancels.contains(t)
      }
  }

  def tasks_to_reject(tasks: List[Task]): List[Task] = {
    val cancels = tasks_to_cancel(tasks).toSet
    val accepts = tasks_to_accept(tasks).toSet
    val accepts_and_cancels = accepts.union(cancels)
    tasks.filter { t =>
      !accepts_and_cancels.contains(t) &&
        not_final(t)
    }
  }
}