package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.{OverBudgetAnswer, LowConfidenceAnswer, Answer}
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.scheduler._
import edu.umass.cs.automan.core.question._

abstract class ScalarValidationStrategy(question: ScalarQuestion)
  extends ValidationStrategy(question) {

  def current_confidence(thunks: List[Thunk]) : Double
  def is_confident(thunks: List[Thunk], round: Int) : Boolean
  def is_done(thunks: List[Thunk], round: Int) = is_confident(thunks, round)

  def answer_selector(thunks: List[Thunk]) : (Question#A,BigDecimal,Double) = {
    val bgrp = biggest_group(thunks)

    // find answer (actually an Option[Question#A]) of the largest group
    val answer_opt: Option[Question#A] = bgrp match { case (group,_) => group }

    // return the top result
    val value = answer_opt.get

    // get the confidence
    val conf = current_confidence(thunks)

    // calculate cost
    val cost = (bgrp match { case (_,ts) => ts }).foldLeft(BigDecimal(0)){ case (acc,t) => acc + t.cost }

    (value, cost, conf)
  }
  private def biggest_group(thunks: List[Thunk]) : (Option[Question#A], List[Thunk]) = {
    val rt = completed_workerunique_thunks(thunks)

    assert(rt.size != 0)

    // group by answer (which is actually an Option[Question#A] because Thunk.answer is Option[Question#A])
    val groups: Map[Option[Question#A], List[Thunk]] = rt.groupBy(_.answer)

    // find answer of the largest group
    groups.maxBy { case(group, ts) => ts.size }
  }
  def rejection_response(thunks: List[Thunk]): String = {
    if (thunks.size == 0) {
      "Your answer is incorrect.  " +
      "We value your feedback, so if you think that we are in error, please contact us."
    } else {
      thunks.head.answer match {
        case Some(a) =>
          "Your answer is incorrect.  The correct answer is '" + a + "'.  " + "" +
          "We value your feedback, so if you think that we are in error, please contact us."
        case None =>
          "Your answer is incorrect.  " +
          "We value your feedback, so if you think that we are in error, please contact us."
      }
    }
  }
  def select_answer(thunks: List[Thunk]) : Question#AA = {
    answer_selector(thunks) match { case (value,cost,conf) =>
      DebugLog("Most popular answer is " + value.toString, LogLevel.INFO, LogType.STRATEGY, question.id)
      Answer(value, cost, conf).asInstanceOf[Question#AA]
    }
  }
  def select_over_budget_answer(thunks: List[Thunk], need: BigDecimal, have: BigDecimal) : Question#AA = {
    // if we've never scheduled anything,
    // there will be no largest group
    if(completed_workerunique_thunks(thunks).size == 0) {
      OverBudgetAnswer(need, have).asInstanceOf[Question#AA]
    } else {
      answer_selector(thunks) match {
        case (value, cost, conf) =>
          DebugLog("Over budget.  Best answer so far is " + value.toString, LogLevel.INFO, LogType.STRATEGY, question.id)
          LowConfidenceAnswer(value, cost, conf).asInstanceOf[Question#AA]
      }
    }
  }
  def thunks_to_accept(thunks: List[Thunk]): List[Thunk] = {
    biggest_group(thunks) match { case (_, ts) => ts }
  }

  def thunks_to_reject(thunks: List[Thunk]): List[Thunk] = {
    val accepts = thunks_to_accept(thunks).toSet
    thunks.filter { t => !accepts.contains(t) }
  }
}