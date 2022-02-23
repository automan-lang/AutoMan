package org.automanlang.core.question

import org.automanlang.core.info.QuestionType
import org.automanlang.core.policy.aggregation.AdversarialPolicy
import org.automanlang.core.policy.price.MLEPricePolicy
import org.automanlang.core.policy.timeout.DoublingTimeoutPolicy

abstract class RadioButtonQuestion extends DiscreteScalarQuestion {
  type A = Symbol
  type QuestionOptionType <: QuestionOption
  type AP = AdversarialPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QuestionOptionType]

  // Experimental JavaScript stuff
  protected var _dimensions: Array[Dimension] = Array()
  def dimensions_=(dim: Array[Dimension]) { _dimensions = dim }
  def dimensions: Array[Dimension] = _dimensions

  override protected[automanlang] def getQuestionType = QuestionType.RadioButtonQuestion

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

  override protected[automanlang] def prettyPrintAnswer(answer: Symbol): String = {
    var optionMap: Map[Symbol, String] = Map[Symbol, String]() // map option symbols to option text
    for(o <- options) optionMap += (o.question_id -> o.question_text)
    optionMap(answer)
  }

  // method that returns an array of the possible responses
  def return_response_possibilities() : Array[String] = {

    // flatten list of combinations
    var arr: Array[String] = Array()
    _options.foreach(o => {
      arr = arr :+ o.question_text
    })

    arr

  }
}
