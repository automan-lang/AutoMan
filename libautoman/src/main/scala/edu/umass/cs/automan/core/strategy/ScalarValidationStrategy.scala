package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.{FreeTextScalarAnswer, CheckboxScalarAnswer, RadioButtonScalarAnswer, ScalarAnswer}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import edu.umass.cs.automan.core.question.{CheckboxQuestion, RadioButtonQuestion, Question}
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}
import java.util.UUID

abstract class ScalarValidationStrategy extends ValidationStrategy {
  var _confidence: Double = 0.95

  def confidence: Double = _confidence
  def confidence_=(c: Double) { _confidence = c }
  def current_confidence: Double
  def is_confident: Boolean
  def is_done = is_confident
  def select_answer(question: Question) : ScalarAnswer = {
    // group by unique symbol specific to each answer type
    val valid_thunks = _thunks.filter{t =>
      t.state == SchedulerState.RETRIEVED ||
      t.state == SchedulerState.PROCESSED
    }

    if (valid_thunks.size == 0) {
      return question match {
        case rbq:RadioButtonQuestion => new RadioButtonScalarAnswer(None, "invalid", 'invalid)
        case cbq:CheckboxQuestion => new CheckboxScalarAnswer(None, "invalid", Set('invalid))
        case _ => throw new Exception("Question type not yet supported.")
      }
    }

    val groups = valid_thunks.groupBy { t => t.answer.asInstanceOf[ScalarAnswer].comparator }
    
    Utilities.DebugLog("Groups = " + groups, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // find the grouping symbol of the largest group
    val gsymb = groups.maxBy { case(opt, as) => as.size }._1

    Utilities.DebugLog("Symbol of largest group is " + gsymb, LogLevel.INFO, LogType.STRATEGY,_computation_id)
    Utilities.DebugLog("classOf Thunk.answer is " + groups(gsymb).head.answer.getClass, LogLevel.INFO, LogType.STRATEGY,_computation_id)

    // return an Answer
    groups(gsymb).head.answer match {
      case rba: RadioButtonScalarAnswer => new RadioButtonScalarAnswer(Some(current_confidence), "aggregated", rba.value)
      case cba: CheckboxScalarAnswer => new CheckboxScalarAnswer(Some(current_confidence), "aggregated", cba.values)
      case fta: FreeTextScalarAnswer => new FreeTextScalarAnswer(Some(current_confidence), "aggregated", fta.value)
      case _ => throw new Exception("Question type not yet supported.")
    }
  }
}