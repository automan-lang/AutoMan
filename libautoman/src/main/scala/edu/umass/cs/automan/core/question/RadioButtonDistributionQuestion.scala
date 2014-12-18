package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.RadioButtonAnswer
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.Utilities

abstract class RadioButtonDistributionQuestion extends DistributionQuestion {
  type QO <: QuestionOption
  type A = RadioButtonAnswer

  protected var _options: List[QO] = List.empty

  def options: List[QO] = _options
  def options_=(os: List[QO]) { _options = os }
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QO]
  def is_likert_scale: Boolean = _dont_randomize_options
  def is_likert_scale_=(b: Boolean) { _dont_randomize_options  = b }
  def question_type = QuestionType.RadioButtonDistributionQuestion
}