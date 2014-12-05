package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.ScalarAnswer
import edu.umass.cs.automan.core.question.DistributionQuestion
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

abstract class DistributionValidationStrategy[Q <: DistributionQuestion, A <: ScalarAnswer, B](question: Q)
  extends ValidationStrategy[Q,A,B](question) {

  def is_done(thunks: List[Thunk[A]]) = {
    val done = completed_workerunique_thunks(thunks).size
    done >= question.num_samples
  }
  override def select_answer(thunks: List[Thunk[A]]): B = {
    // just return all retrieved answers
    // asInstanceOf[B] necessary because Scala does not
    // know that B is always Set[A]
    completed_workerunique_thunks(thunks).map { t => t.answer }.toSet.asInstanceOf[B]
  }
  override def thunks_to_accept(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    val unaccepted_thunks = thunks.filter(_.state == SchedulerState.RETRIEVED)
    if (completed_workerunique_thunks(thunks).size >= question.num_samples) {
      unaccepted_thunks
    } else {
      throw new PrematureValidationCompletionException("thunks_to_accept", this.getClass.toString)
    }
  }
  override def thunks_to_reject(thunks: List[Thunk[A]]): List[Thunk[A]] = List.empty[Thunk[A]] // never reject
}