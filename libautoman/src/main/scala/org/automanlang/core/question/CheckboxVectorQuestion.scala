package org.automanlang.core.question

import org.automanlang.core.answer._
import org.automanlang.core.info.QuestionType

abstract class CheckboxVectorQuestion extends VectorQuestion {
  type A = Set[Symbol]
  type AA = Answers[A]
  type O = VectorOutcome[A]
  type QuestionOptionType <: QuestionOption

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = {
    val base = BigInt(2)
    base.pow(options.size)
  }
  def randomized_options: List[QuestionOptionType]

  override protected[automanlang] def getQuestionType = QuestionType.CheckboxDistributionQuestion

  override protected[automanlang] def prettyPrintAnswer(answer: Set[Symbol]): String = {
    var optionMap: Map[Symbol, String] = Map[Symbol, String]() // map option symbols to option text
    for(o <- options) optionMap += (o.question_id -> o.question_text)

    "(" + answer.map(optionMap(_)).mkString(", ") + ")"
  }
}
