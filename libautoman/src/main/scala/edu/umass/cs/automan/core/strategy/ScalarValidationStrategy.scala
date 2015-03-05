package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.{ScalarOverBudget, ScalarAnswer, AbstractScalarAnswer}
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.scheduler._
import edu.umass.cs.automan.core.question._

abstract class ScalarValidationStrategy[A](question: Question[A])
  extends ValidationStrategy[A](question) {

  def current_confidence(thunks: List[Thunk[A]]) : Double
  def is_confident(thunks: List[Thunk[A]]) : Boolean
  def is_done(thunks: List[Thunk[A]]) = is_confident(thunks)

  private def biggest_group(thunks: List[Thunk[A]]) : (Option[A], List[Thunk[A]]) = {
    val rt = completed_workerunique_thunks(thunks)

    assert(rt.size != 0)

    // group by answer (which is actually an Option[A] because Thunk.answer is Option[A])
    val groups: Map[Option[A], List[Thunk[A]]] = rt.groupBy(_.answer)

    // find answer of the largest group
    groups.maxBy { case(group, ts) => ts.size }
  }
  def answer_selector(thunks: List[Thunk[A]]) : (A,BigDecimal,Double) = {
    val bgrp = biggest_group(thunks)

    // find answer (actually an Option[A]) of the largest group
    val answer_opt: Option[A] = bgrp match { case (group,_) => group }

    // return the top result
    val value = answer_opt.get

    // get the confidence
    val conf = current_confidence(thunks)

    // calculate cost
    val cost = (bgrp match { case (_,ts) => ts }).foldLeft(BigDecimal(0)){ case (acc,t) => acc + t.cost }

    (value, cost, conf)
  }
  def select_answer(thunks: List[Thunk[A]]) : ScalarAnswer[A] = {
    answer_selector(thunks) match { case (value,cost,conf) =>
      DebugLog("Most popular answer is " + value.toString, LogLevel.INFO, LogType.STRATEGY, question.id)
      ScalarAnswer(value,cost,conf)
    }
  }
  def select_over_budget_answer(thunks: List[Thunk[A]]) : ScalarOverBudget[A] = {
    answer_selector(thunks) match { case (value,cost,conf) =>
      DebugLog("Over budget.  Best answer so far is " + value.toString, LogLevel.INFO, LogType.STRATEGY, question.id)
      ScalarOverBudget(value,cost,conf)
    }
  }
  override def thunks_to_accept(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    biggest_group(thunks) match { case (_, ts) => ts }
  }

  override def thunks_to_reject(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    val accepts = thunks_to_accept(thunks).toSet
    thunks.filter { t => !accepts.contains(t) }
  }
}