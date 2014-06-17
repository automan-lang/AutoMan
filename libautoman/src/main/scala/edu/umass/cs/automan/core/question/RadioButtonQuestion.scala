package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.RadioButtonScalarAnswer

abstract class RadioButtonQuestion extends ScalarQuestion {
  type QO <: QuestionOption
  type A = RadioButtonScalarAnswer

  protected var _options: List[QO]

  def options: List[QO]
  def options_=(os: List[QO])
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QO]
}