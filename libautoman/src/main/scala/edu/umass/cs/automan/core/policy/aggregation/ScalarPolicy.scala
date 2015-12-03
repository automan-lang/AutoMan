package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.answer.{Answer, LowConfidenceAnswer, OverBudgetAnswer}
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.scheduler._

abstract class ScalarPolicy(question: Question)
  extends AggregationPolicy(question) {

  def current_confidence(tasks: List[Task]) : Double
  def is_confident(tasks: List[Task], num_hypotheses: Int) : Boolean
  def is_done(tasks: List[Task]) = {
    is_confident(tasks, numComparisons(tasks))
  }

  def not_final(task: Task) : Boolean = {
    task.state != SchedulerState.ACCEPTED &&
    task.state != SchedulerState.REJECTED &&
    task.state != SchedulerState.CANCELLED &&
    task.state != SchedulerState.TIMEOUT
  }

  def numComparisons(tasks: List[Task]) : Int = {
    // the number of rounds completed == the number of comparisons
    if (tasks.nonEmpty) { tasks.map(_.round).max } else { 1 }
  }
}