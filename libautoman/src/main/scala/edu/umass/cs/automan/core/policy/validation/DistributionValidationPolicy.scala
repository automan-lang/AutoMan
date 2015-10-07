package edu.umass.cs.automan.core.policy.validation

import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.scheduler._

abstract class DistributionValidationPolicy(question: DistributionQuestion)
  extends ValidationPolicy(question) {

  def is_done(tasks: List[Task]) = {
    val done = completed_workerunique_tasks(tasks).size
    done >= question.sample_size
  }
  def rejection_response(tasks: List[Task]): String = {
    "We can only accept a single answer per worker."
  }
  def select_answer(tasks: List[Task]) : Question#AA = {
    val valid_tasks: List[Task] = completed_workerunique_tasks(tasks)
    val distribution: Set[(String,Question#A)] = valid_tasks.map { t => (t.worker_id.get, t.answer.get) }.toSet
    val cost: BigDecimal = valid_tasks.filterNot(_.from_memo).foldLeft(BigDecimal(0)){ case (acc,t) => acc + t.cost }
    Answers(distribution, cost).asInstanceOf[Question#AA]
  }
  def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal) : Question#AA = {
    val valid_tasks: List[Task] = completed_workerunique_tasks(tasks)
    if (valid_tasks.size == 0) {
      OverBudgetAnswers(need, have).asInstanceOf[Question#AA]
    } else {
      val distribution: Set[(String, Question#A)] = valid_tasks.map { t => (t.worker_id.get, t.answer.get) }.toSet
      val cost: BigDecimal = valid_tasks.map { t => t.cost }.foldLeft(BigDecimal(0)) { (acc, c) => acc + c }
      IncompleteAnswers(distribution, cost).asInstanceOf[Question#AA]
    }
  }
  override def tasks_to_accept(tasks: List[Task]): List[Task] = {
    completed_workerunique_tasks(tasks)
      .filter(not_final)
  }
  override def tasks_to_reject(tasks: List[Task]): List[Task] = {
    val accepts = tasks_to_accept(tasks).toSet
    tasks.filter { t =>
      !accepts.contains(t) &&
      not_final(t)
    }
  }

  def not_final(task: Task) : Boolean = {
    task.state != SchedulerState.ACCEPTED &&
      task.state != SchedulerState.REJECTED &&
      task.state != SchedulerState.CANCELLED
  }
}