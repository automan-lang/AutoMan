package org.automanlang.core.question

import org.automanlang.core.info.QuestionType
import org.automanlang.core.policy.aggregation.AdversarialPolicy
import org.automanlang.core.policy.price.MLEPricePolicy
import org.automanlang.core.policy.timeout.DoublingTimeoutPolicy

abstract class FreeTextQuestion extends DiscreteScalarQuestion {
  type A = String
  type QuestionOptionType <: QuestionOption
  type AP = AdversarialPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

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

  override protected[automanlang] def getQuestionType = QuestionType.FreeTextQuestion

  // private methods
  override private[automanlang] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new AP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }
  override private[automanlang] def init_price_policy(): Unit = {
    _price_policy_instance = _price_policy match {
      case None => new PP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }
  override private[automanlang] def init_timeout_policy(): Unit = {
    _timeout_policy_instance = _timeout_policy match {
      case None => new TP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override protected[automanlang] def prettyPrintAnswer(answer: String): String = answer
}
