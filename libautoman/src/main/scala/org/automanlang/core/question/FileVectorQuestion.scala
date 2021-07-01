package org.automanlang.core.question

import org.automanlang.core.answer._
import org.automanlang.core.info.QuestionType

abstract class FileVectorQuestion extends VectorQuestion {
  type A = String
  type AA = Answers[A]
  type O = VectorOutcome[A]
  type QuestionOptionType <: QuestionOption

  protected var _allow_empty: Boolean = false
  protected var _num_possibilities: BigInt = 1000
  protected var _pattern: Option[String] = None
  protected var _pattern_error_text: String = ""

  def allow_empty_pattern_=(ae: Boolean) { _allow_empty = ae }
  def allow_empty_pattern: Boolean = _allow_empty
  def num_possibilities: BigInt = _pattern match {
    case Some(p) =>
      val count = PictureClause(p, _allow_empty)._2
      if (count > 1000) 1000 else count
    case None => 1000
  }
  def pattern: String = _pattern match { case Some(p) => p; case None => ".*" }
  def pattern_=(p: String) { _pattern = Some(p) }
  def pattern_error_text: String = _pattern_error_text
  def pattern_error_text_=(p: String) { _pattern_error_text = p }
  def regex: String = _pattern match {
    case Some(p) => PictureClause(p, _allow_empty)._1
    case None => "^.*$"
  }

  override protected[automanlang] def getQuestionType = QuestionType.FreeTextDistributionQuestion

  override protected[automanlang] def prettyPrintAnswer(answer: String): String = answer
}
