package org.automanlang.core.question

import org.automanlang.core.info.QuestionType
import org.automanlang.core.answer._

abstract class SurveyQuestion extends VectorQuestion {
  type A = List[Any]
  type AA = Answers[A]
  type O = VectorOutcome[A]
  type QuestionOptionType <: QuestionOption

  protected var _questions: List[Question] = List()

  def questions: List[Question] = _questions
  def questions_=(q: List[Question]) { _questions = q }

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = {
    val base = BigInt(2)
    base.pow(options.size)
  }
  def randomized_options: List[QuestionOptionType]

  override protected[automanlang] def getQuestionType = QuestionType.SurveyQuestion

  override protected[automanlang] def prettyPrintAnswer(answer: List[Any]): String = {
//    var optionMap: Map[Symbol, String] = Map[Symbol, String]() // map option symbols to option text
//    for(o <- options) optionMap += (o.question_id -> o.question_text)
//
//    "(" + answer.map(optionMap(_)).mkString(", ") + ")"

    // NEEDS TO BE CHANGED
    //"(test answer)"
    var answerString = ""
    answer.foreach(a => answerString += a.toString())

    answerString

  }
}
