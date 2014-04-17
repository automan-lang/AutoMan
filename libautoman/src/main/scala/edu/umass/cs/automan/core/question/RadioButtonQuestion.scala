package edu.umass.cs.automan.core.question

abstract class RadioButtonQuestion extends Question {
  type QO <: QuestionOption
  protected var _options: List[QO]

  def options: List[QO]
  def options_=(os: List[QO])
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QO]
}