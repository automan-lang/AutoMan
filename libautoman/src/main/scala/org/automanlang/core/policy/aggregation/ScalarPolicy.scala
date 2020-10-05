package org.automanlang.core.policy.aggregation

import org.automanlang.core.answer.{Answer, LowConfidenceAnswer, OverBudgetAnswer}
import org.automanlang.core.logging._
import org.automanlang.core.question._
import org.automanlang.core.scheduler._

abstract class ScalarPolicy(question: Question)
  extends AggregationPolicy(question) {

  def current_confidence(tasks: List[Task]) : Double
  def is_confident(tasks: List[Task], num_comparisons: Int) : (Boolean, Int)
  def is_done(tasks: List[Task], num_comparisons: Int) : (Boolean,Int) = {
    is_confident(tasks, num_comparisons)
  }
}