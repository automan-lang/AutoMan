package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.RadioButtonDistributionAnswer

abstract class RadioButtonDistributionQuestion extends DistributionQuestion {
  type QO <: QuestionOption
  type A = RadioButtonDistributionAnswer

  protected var _options: List[QO]

  def options: List[QO]
  def options_=(os: List[QO])
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QO]
}