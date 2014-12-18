package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.CheckboxAnswer
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.Utilities

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
  def question_type = QuestionType.CheckboxQuestion
  def randomized_options: List[QO]
}