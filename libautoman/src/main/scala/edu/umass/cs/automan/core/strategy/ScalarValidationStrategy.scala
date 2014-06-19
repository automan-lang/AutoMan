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
  def current_confidence: Double
  def is_confident: Boolean
  def is_done = is_confident
  def select_answer : B = {
    val rt = valid_thunks // only retrieved and memo-recalled; only earliest submission per-worker

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
    val groups = rt.groupBy { t => t.answer.comparator }
    
    Utilities.DebugLog("Groups = " + groups, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // find the grouping symbol of the largest group
    val gsymb = groups.maxBy { case(opt, as) => as.size }._1

    Utilities.DebugLog("Symbol of largest group is " + gsymb, LogLevel.INFO, LogType.STRATEGY,_computation_id)
    Utilities.DebugLog("classOf Thunk.answer is " + groups(gsymb).head.answer.getClass, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // return the top Answer
    // TODO: can I get rid of the typecast?
    _selected_answer = Some(groups(gsymb).head.answer.final_answer(Some(current_confidence)).asInstanceOf[A])
    _selected_answer.get.asInstanceOf[B]
  }

  override def thunks_to_accept: List[Thunk[A]] = {
    _selected_answer match {
      case Some(answer) =>
        _thunks
          .filter( _.state == SchedulerState.RETRIEVED )
          .filter( t => t.answer.sameAs(answer)) // note that we accept all of a worker's matching submissions
                                                 // even if we have to accept duplicate submissions
      case None => throw new PrematureValidationCompletionException("thunks_to_accept", this.getClass.toString)
    }
  }

  override def thunks_to_reject: List[Thunk[A]] = {
    _selected_answer match {
      case Some(answer) =>
        _thunks
          .filter( _.state == SchedulerState.RETRIEVED )
          .filter( t => !t.answer.sameAs(answer))
      case None => throw new PrematureValidationCompletionException("thunks_to_reject", this.getClass.toString)
    }
  }
}