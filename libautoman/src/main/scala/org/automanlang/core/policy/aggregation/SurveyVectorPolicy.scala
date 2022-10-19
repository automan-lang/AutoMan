package org.automanlang.core.policy.aggregation

import org.automanlang.core.answer._
import org.automanlang.core.question._
import org.automanlang.core.scheduler._

abstract class SurveyVectorPolicy(question: FakeSurvey)
  extends AggregationPolicy(question) {

  def is_done(tasks: List[Task], num_comparisons: Int): (Boolean, Int) = {
    val done = completed_workerunique_tasks(tasks).size
    // num_comparisons is irrelevant for termination based
    // on a fixed sample size but bump anyway for consistency
    (done >= question.sample_size, num_comparisons + 1)
  }
  def rejection_response(tasks: List[Task]): String = {
    "We can only accept a single answer per worker."
  }
  def select_answer(tasks: List[Task], num_comparisons: Int) : Question#AA = {
    val valid_tasks: List[Task] = completed_workerunique_tasks(tasks)
    // filtered dist
    val distribution: Set[(String,Question#A)] = valid_tasks.map { t => (t.worker_id.get, t.answer.get) }.toSet
    val metadatas: Map[String, SurveyTaskMetadata] = valid_tasks.map {t => {(t.worker_id.get, SurveyTaskMetadata(t.worker_id.get, t.cost, 0, likely_noise = false))}}.toMap
    // raw distribution
    val dist = getDistribution(tasks)
    val cost: BigDecimal = valid_tasks.filterNot(_.from_memo).foldLeft(BigDecimal(0)){ case (acc,t) => acc + t.cost }
    SurveyAnswers(distribution, metadatas, cost, question, dist).asInstanceOf[Question#AA]
  }
  def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal, num_comparisons: Int) : Question#AA = {
    val valid_tasks: List[Task] = completed_workerunique_tasks(tasks)
    if (valid_tasks.isEmpty) {
      SurveyOverBudgetAnswers(need, have, question).asInstanceOf[Question#AA]
    } else {
      val distribution: Set[(String, Question#A)] = valid_tasks.map { t => (t.worker_id.get, t.answer.get) }.toSet
      val cost: BigDecimal = valid_tasks.map { t => t.cost }.foldLeft(BigDecimal(0)) { (acc, c) => acc + c }
      // distribution
      val dist = getDistribution(tasks)
      SurveyIncompleteAnswers(distribution, cost, question, dist).asInstanceOf[Question#AA]
    }
  }
  override def tasks_to_accept(tasks: List[Task]): List[Task] = {
    val cancels = tasks_to_cancel(tasks).toSet
    completed_workerunique_tasks(tasks)
      .filter { t =>
        not_final(t) &&
        !cancels.contains(t)
      }
  }
  override def tasks_to_reject(tasks: List[Task]): List[Task] = {
    val accepts = tasks_to_accept(tasks).toSet
    tasks.filter { t =>
      !accepts.contains(t) &&
      not_final(t)
    }
  }
}
