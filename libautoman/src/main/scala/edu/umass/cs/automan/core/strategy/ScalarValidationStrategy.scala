package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.scheduler._
import edu.umass.cs.automan.core.question._

abstract class ScalarValidationStrategy[A](question: Question[A])
  extends ValidationStrategy[A](question) {

  def current_confidence(thunks: List[Thunk[A]]) : Double
  def is_confident(thunks: List[Thunk[A]]) : Boolean
  def is_done(thunks: List[Thunk[A]]) = is_confident(thunks)
  def select_answer(thunks: List[Thunk[A]]) : Option[SchedulerResult[A]] = {
    val rt = completed_workerunique_thunks(thunks) // only retrieved and memo-recalled; only earliest submission per-worker

    if (rt.size == 0) {
      None
    }

    // group by answer (which is actually an Option[A] because Thunk.answer is Option[A])
    val groups: Map[Option[A], List[Thunk[A]]] = rt.groupBy(_.answer)
    
    DebugLog("Groups = " + groups, LogLevel.INFO, LogType.STRATEGY, question.id)

    // find answer of the largest group
    val gsymb: Option[A] = groups.maxBy { case(opt, as) => as.size }._1

    DebugLog("Most popular answer is " + gsymb, LogLevel.INFO, LogType.STRATEGY, question.id)
    DebugLog("classOf Thunk.answer is " + groups(gsymb).head.answer.get.getClass, LogLevel.INFO, LogType.STRATEGY, question.id)

    // return the top result
    val selected_answer = Some(groups(gsymb).head.answer.get)
    Some(SchedulerResult(selected_answer.get, ???, current_confidence(thunks)))
  }

  override def thunks_to_accept(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    if (question.dont_reject) {
      thunks
    } else {
      val selected_answer = select_answer(thunks)
      selected_answer match {
        case Some(answer) =>
          thunks
            .filter(_.state == SchedulerState.RETRIEVED)
            .filter(_.answer.get == answer) // note that we accept all of a worker's matching submissions
        // even if we have to accept duplicate submissions
        case None => throw new PrematureValidationCompletionException("thunks_to_accept", this.getClass.toString)
      }
    }
  }

  override def thunks_to_reject(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    if (question.dont_reject) {
      List.empty
    } else {
      val selected_answer = select_answer(thunks)
      selected_answer match {
        case Some(answer) =>
          thunks
            .filter(_.state == SchedulerState.RETRIEVED)
            .filter(_.answer.get != answer)
        case None => throw new PrematureValidationCompletionException("thunks_to_reject", this.getClass.toString)
      }
    }
  }
}