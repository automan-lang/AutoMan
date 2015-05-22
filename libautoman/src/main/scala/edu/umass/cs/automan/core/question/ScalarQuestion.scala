package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.{ScalarOutcome, AbstractScalarAnswer}
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy
import edu.umass.cs.automan.core.policy.validation.DefaultScalarPolicy

abstract class ScalarQuestion extends Question {
  type AA = AbstractScalarAnswer[A]
  type O = ScalarOutcome[A]
  type VP = DefaultScalarPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  protected var _confidence: Double = 0.95

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  // private methods
  override private[automan] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new VP(this)
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
}
