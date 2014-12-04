package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.CheckboxAnswer
import edu.umass.cs.automan.adapters.MTurk.question.MTQuestionOption
import edu.umass.cs.automan.core.strategy.DefaultScalarStrategy

abstract class CheckboxQuestion extends ScalarQuestion {
  type QO <: QuestionOption
  type A = CheckboxAnswer

  protected var _options: List[QO]

  def options: List[QO]
  def options_=(os: List[QO])
  def num_possibilities: BigInt = {
    val base = BigInt(2)
    base.pow(options.size)
  }
  def randomized_options: List[QO]
}