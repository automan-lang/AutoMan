package org.automanlang.core.question

import org.automanlang.core.AutomanAdapter
import org.automanlang.core.answer._
import org.automanlang.core.policy.price.FixedPricePolicy
import org.automanlang.core.policy.timeout.FixedTimeoutPolicy
import org.automanlang.core.policy.aggregation.SurveyPolicy
import org.automanlang.core.AutomanAdapter

import scala.concurrent.ExecutionContext.Implicits.global

abstract class MixedQuestion extends Question {
  type AA <: AbstractMixedAnswer[A]
  type O <: MixedOutcome[A]
  type AP = SurveyPolicy
  type PP = FixedPricePolicy
  type TP = FixedTimeoutPolicy

  private var _sample_size: Int = 30

  def sample_size_=(n: Int) { _sample_size = n }
  def sample_size : Int = _sample_size

  protected[automanlang] def getOutcome(adapter: AutomanAdapter) : O = {
    MixedOutcome(this, schedulerFuture(adapter)).asInstanceOf[O]
  }
  protected[automanlang] def composeOutcome(o: O, adapter: AutomanAdapter) : O = {
    // unwrap future from previous Outcome
    val f = o.f map {
      case AnswersM(values, _, id, dist) =>
        AnswersM(
          values,
          BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
          id,
          dist
        )
      case _ => startScheduler(adapter)
    }
    MixedOutcome(this, f).asInstanceOf[O]
  }

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
}
