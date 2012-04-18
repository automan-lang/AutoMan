package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.RadioButtonAnswer

abstract class RadioButtonQuestion[QO <: QuestionOption] extends Question {
  protected var _options: List[QO]

  def options: List[QO]
  def options_=(os: List[QO])
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QO]
}