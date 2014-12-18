package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.{FreeTextAnswer, CheckboxAnswer, RadioButtonAnswer, ScalarAnswer}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.question._
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

abstract class ScalarValidationStrategy[Q <: ScalarQuestion, A <: ScalarAnswer, B](question: Q)
  extends ValidationStrategy[Q,A,B](question) {

  var _confidence: Double = 0.95
  var _selected_answer: Option[A] = None

  def confidence: Double = _confidence
  def confidence_=(c: Double) { _confidence = c }
  def current_confidence(thunks: List[Thunk[A]]) : Double
  def is_confident(thunks: List[Thunk[A]]) : Boolean
  def is_done(thunks: List[Thunk[A]]) = is_confident(thunks)
  def select_answer(thunks: List[Thunk[A]]) : B = {
    val rt = completed_workerunique_thunks(thunks) // only retrieved and memo-recalled; only earliest submission per-worker

    // TODO: this is ugly and I don't remember why it's important
    if (rt.size == 0) {
      return question match {
        case rbq:RadioButtonQuestion => new RadioButtonAnswer(None, "invalid", 'invalid).asInstanceOf[B]
        case ftq:FreeTextQuestion => new FreeTextAnswer(None, "invalid", 'invalid).asInstanceOf[B]
        case cbq:CheckboxQuestion => new CheckboxAnswer(None, "invalid", Set('invalid)).asInstanceOf[B]
        case _ => throw new Exception("Question type not yet supported.")
      }
    }

    // group by unique symbol specific to each answer type
    val groups = rt.groupBy { t => t.answer.get.comparator }
    
    Utilities.DebugLog("Groups = " + groups, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // find the grouping symbol of the largest group
    val gsymb = groups.maxBy { case(opt, as) => as.size }._1

    Utilities.DebugLog("Most popular answer is " + gsymb, LogLevel.INFO, LogType.STRATEGY,_computation_id)
    Utilities.DebugLog("classOf Thunk.answer is " + groups(gsymb).head.answer.get.getClass, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // return the top Answer
    _selected_answer = Some(groups(gsymb).head.answer.get.final_answer(Some(current_confidence(thunks))).asInstanceOf[A])
    _selected_answer.get.asInstanceOf[B]
  }

  override def thunks_to_accept(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    _selected_answer match {
      case Some(answer) =>
        thunks
          .filter( _.state == SchedulerState.RETRIEVED )
          .filter( t => t.answer.get.sameAs(answer)) // note that we accept all of a worker's matching submissions
                                                 // even if we have to accept duplicate submissions
      case None => throw new PrematureValidationCompletionException("thunks_to_accept", this.getClass.toString)
    }
  }

  override def thunks_to_reject(thunks: List[Thunk[A]]): List[Thunk[A]] = {
    _selected_answer match {
      case Some(answer) =>
        thunks
          .filter( _.state == SchedulerState.RETRIEVED )
          .filter( t => !t.answer.get.sameAs(answer))
      case None => throw new PrematureValidationCompletionException("thunks_to_reject", this.getClass.toString)
    }
  }
}