package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.info.QuestionType

abstract class RadioButtonVectorQuestion extends VectorQuestion {
  type A = Symbol
  type AA = Answers[A]
  type O = VectorOutcome[A]
  type QuestionOptionType <: QuestionOption

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QuestionOptionType]

  override protected[automan] def getQuestionType = QuestionType.RadioButtonDistributionQuestion

  override protected[automan] def prettyPrintAnswer(answer: Symbol): String = {
    answer.toString().drop(1)
  }
}
