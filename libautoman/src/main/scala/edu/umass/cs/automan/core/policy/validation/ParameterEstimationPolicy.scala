package edu.umass.cs.automan.core.policy.validation

import edu.umass.cs.automan.core.question.{ScalarQuestion, Question}
import edu.umass.cs.automan.core.scheduler.Task

class ParameterEstimationPolicy(delta: Int, question: ScalarQuestion) extends ScalarValidationPolicy(question) {
  override def answer_selector(tasks: List[Task]): (Question#A, BigDecimal, Double) = super.answer_selector(tasks)

  override def is_done(tasks: List[Task]): Boolean = super.is_done(tasks)

  override def not_final(task: Task): Boolean = super.not_final(task)

  override def rejection_response(tasks: List[Task]): String = super.rejection_response(tasks)

  override def select_answer(tasks: List[Task]): Question#AA = super.select_answer(tasks)

  override def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal): Question#AA = super.select_over_budget_answer(tasks, need, have)

  override def tasks_to_accept(tasks: List[Task]): List[Task] = super.tasks_to_accept(tasks)

  override def tasks_to_reject(tasks: List[Task]): List[Task] = super.tasks_to_reject(tasks)

  override def current_confidence(tasks: List[Task]): Double = ???

  override def is_confident(tasks: List[Task], num_hypotheses: Int): Boolean = ???

  /**
    * Computes the number of tasks needed to satisfy the quality-control
    * algorithm given the already-collected list of tasks. Returns only
    * newly-created tasks.
    *
    * @param tasks The complete list of previously-scheduled tasks
    * @param suffered_timeout True if any of the latest batch of tasks suffered a timeout.
    * @return A list of new tasks to schedule on the backend.
    */
  override def spawn(tasks: List[Task], suffered_timeout: Boolean): List[Task] = ???
}
