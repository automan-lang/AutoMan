package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{MultiEstimate, MultiEstimationOutcome, AbstractMultiEstimate}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.policy.aggregation.MultiBootstrapEstimationPolicy
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy
import edu.umass.cs.automan.core.question.confidence.ConfidenceInterval
import scala.concurrent.ExecutionContext.Implicits.global

abstract class MultiEstimationQuestion extends Question {
  type A = Array[Double]
  type AA = AbstractMultiEstimate
  type O = MultiEstimationOutcome
  type AP = MultiBootstrapEstimationPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  protected var _confidence: Double = 0.95
  protected var _default_sample_size: Int = 12
  protected var _dimensions: Array[Dimension] = Array()
  protected var _estimator: Seq[Array[Double]] => Array[Double] = {
    // use whatever default is set for each estimation question
    dss => dss.zipWithIndex.map { case (ds,i) =>
      _dimensions(i).estimator(ds)
    }.toArray
  }

  def cardinality: Int = dimensions.length
  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence
  def confidence_region: Array[ConfidenceInterval] = _dimensions.map(_.confidence_interval)
  def default_sample_size: Int = _default_sample_size
  def default_sample_size_=(n: Int) { _default_sample_size = n }
  def dimensions_=(dim: Array[Dimension]) { _dimensions = dim }
  def dimensions: Array[Dimension] = _dimensions
  def estimator: Seq[Array[Double]] => Array[Double] = _estimator
  def estimator_=(fn: Seq[Array[Double]] => Array[Double]) { _estimator = fn }

  override protected[automan] def composeOutcome(o: MultiEstimationOutcome, adapter: AutomanAdapter): O = {
    // unwrap future from previous Outcome
    val f = o.f map {
      case MultiEstimate(values, lows, highs, cost, conf, id) =>
        if (this.confidence <= conf) {
          MultiEstimate(
            values,
            lows,
            highs,
            BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
            conf,
            id
          )
        } else {
          startScheduler(adapter)
        }
      case _ => startScheduler(adapter)
    }
    MultiEstimationOutcome(f)
  }

  override protected[automan] def getOutcome(adapter: AutomanAdapter): O = {
    MultiEstimationOutcome(schedulerFuture(adapter))
  }

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

  override protected[automan] def getQuestionType: QuestionType = QuestionType.MultiEstimationQuestion
}
