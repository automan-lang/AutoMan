package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.policy.price.FixedPricePolicy
import edu.umass.cs.automan.core.policy.timeout.FixedTimeoutPolicy
import edu.umass.cs.automan.core.policy.aggregation.SimpleVectorPolicy
import scala.concurrent.ExecutionContext.Implicits.global

abstract class VectorQuestion extends Question {
  type AA <: AbstractVectorAnswer[A]
  type O <: VectorOutcome[A]
  type AP = SimpleVectorPolicy
  type PP = FixedPricePolicy
  type TP = FixedTimeoutPolicy

  private var _sample_size: Int = 30

  def sample_size_=(n: Int) { _sample_size = n }
  def sample_size : Int = _sample_size

  protected[automan] def getOutcome(adapter: AutomanAdapter) : O = {
    VectorOutcome(schedulerFuture(adapter)).asInstanceOf[O]
  }
  protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter) : O = {
    // unwrap future from previous Outcome
    val f = o.f map {
      case Answers(values, _, id, dist) =>
          Answers(
            values,
            BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
            id,
            dist
          )
      case _ => startScheduler(adapter)
    }
    VectorOutcome(f).asInstanceOf[O]
  }

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
}
