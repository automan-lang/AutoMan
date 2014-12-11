package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.RadioButtonAnswer
import edu.umass.cs.automan.core.info.QuestionType

abstract class RadioButtonQuestion extends ScalarQuestion {
  type QO <: QuestionOption
  type A = RadioButtonAnswer

  protected var _options: List[QO]

  def options: List[QO]
  def options_=(os: List[QO])
  def num_possibilities: BigInt = BigInt(_options.size)
  def question_type = QuestionType.RadioButtonQuestion
  def randomized_options: List[QO]
}