package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.policy.aggregation.AdversarialPolicy
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy

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

  override protected[automan] def getQuestionType = QuestionType.RadioButtonQuestion

  // private methods
  override private[automan] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new AP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }
  override private[automan] def init_price_policy(): Unit = {
    _price_policy_instance = _price_policy match {
      case None => new PP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }
  override private[automan] def init_timeout_policy(): Unit = {
    _timeout_policy_instance = _timeout_policy match {
      case None => new TP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override protected[automan] def prettyPrintAnswer(answer: Symbol): String = {
    answer.toString().drop(1)
  }
}
