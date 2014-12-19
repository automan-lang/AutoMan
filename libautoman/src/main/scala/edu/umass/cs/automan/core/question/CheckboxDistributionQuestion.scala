package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.CheckboxAnswer
import edu.umass.cs.automan.core.info.QuestionType

abstract class CheckboxDistributionQuestion extends DistributionQuestion {
  type QO <: QuestionOption
  override type A = CheckboxAnswer

  protected var _options: List[QO] = List.empty

  def options: List[QO] = _options
  def options_=(os: List[QO]) { _options = os }
  def randomized_options: List[QO]
  override def question_type = QuestionType.CheckboxDistributionQuestion
}
