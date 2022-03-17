package org.automanlang.core.question

import org.automanlang.core.AutomanAdapter
import org.automanlang.core.answer._
import org.automanlang.core.info.QuestionType
import org.automanlang.core.policy.aggregation.BootstrapEstimationPolicy
import org.automanlang.core.policy.price.MLEPricePolicy
import org.automanlang.core.policy.timeout.DoublingTimeoutPolicy
import org.automanlang.core.question.confidence._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class EstimationQuestion extends Question {
  type A = Double
  type AA = AbstractEstimate
  type O = EstimationOutcome
  type AP = BootstrapEstimationPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  protected var _confidence: Double = 0.95
  protected var _confidence_interval: ConfidenceInterval = UnconstrainedCI()
  protected var _default_sample_size: Int = 12
  protected var _estimator: Seq[Double] => Double = {
    // by default, use the mean
    ds => ds.sum / ds.length
  }
  protected var _min_value: Option[Double] = None
  protected var _max_value: Option[Double] = None

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence
  def confidence_interval_=(ci: ConfidenceInterval) { _confidence_interval = ci }
  def confidence_interval: ConfidenceInterval = _confidence_interval
  def default_sample_size: Int = _default_sample_size
  def default_sample_size_=(n: Int) { _default_sample_size = n }
  def estimator: Seq[Double] => Double = _estimator
  def estimator_=(fn: Seq[Double] => Double) { _estimator = fn }
  def max_value: Double = _max_value match {
    case Some(v) => v
    case None => Double.PositiveInfinity
  }
  def max_value_=(max: Double) { _max_value = Some(max) }
  def min_value: Double = _min_value match {
    case Some(v) => v
    case None => Double.NegativeInfinity
  }
  def min_value_=(min: Double) { _min_value = Some(min) }

  protected[automanlang] def getQuestionType = QuestionType.EstimationQuestion
  protected[automanlang] def getOutcome(adapter: AutomanAdapter) : O = {
    EstimationOutcome(this, schedulerFuture(adapter))
  }
  protected[automanlang] def composeOutcome(o: O, adapter: AutomanAdapter) : O = {
    // unwrap future from previous Outcome
    val f = o.f map {
      case Estimate(value, low, high, cost, conf, id, dist) =>
        if (this.confidence <= conf) {
          Estimate(
            value,
            low,
            high,
            BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
            conf,
            id,
            dist
          )
        } else {
          startScheduler(adapter)
        }
      case _ => startScheduler(adapter)
    }
    EstimationOutcome(this, f)
  }

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

  override protected[automanlang] def prettyPrintAnswer(answer: Double): String = {
    answer.toString
  }

  protected[automanlang] def cloneWithConfidence(conf: Double) : EstimationQuestion

}
