package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.answer.{Answer, LowConfidenceAnswer, OverBudgetAnswer}
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.scheduler._

abstract class ScalarPolicy(question: Question)
  extends AggregationPolicy(question) {

  def current_confidence(tasks: List[Task]) : Double
  def is_confident(tasks: List[Task], num_comparisons: Int) : (Boolean, Int)
  def is_done(tasks: List[Task], num_comparisons: Int) : (Boolean,Int) = {
    is_confident(tasks, num_comparisons)
  }
}