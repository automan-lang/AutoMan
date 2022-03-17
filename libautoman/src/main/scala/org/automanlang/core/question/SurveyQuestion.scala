package org.automanlang.core.question

import org.automanlang.core.info.QuestionType
import org.automanlang.core.answer._

abstract class SurveyQuestion extends MixedQuestion {
  type A = List[Any]
  type AA = AnswersM[A]
  type O = MixedOutcome[A]
  type QuestionOptionType <: QuestionOption

  protected var _questions: List[Question] = List()

//  protected var _innerText: Option[String] = None
//  def innerText: String = _innerText match { case Some(t) => t; case None => "Question not specified." }
//  def innerText_=(s: String) { _innerText = Some(s) }

  def questions: List[Question] = _questions
  def questions_=(q: List[Question]) { _questions = q }

  protected var _csv_file: String = null;

  def csv_file: String = _csv_file
  def csv_file_=(c: String) { _csv_file = c }

  protected var _grammar: Map[String, List[String]] = Map()
  def grammar: Map[String, List[String]] = _grammar
  def grammar_=(g: Map[String, List[String]]) { _grammar = g }

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
