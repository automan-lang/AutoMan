package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractScalarAnswer, ScalarOutcome}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.policy.aggregation.BootstrapEstimationPolicy
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy
import edu.umass.cs.automan.core.question.confidence._
import edu.umass.cs.automan.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class EstimationQuestion extends Question {
  type A = Double
  type AA = AbstractScalarAnswer[A]
  type O = ScalarOutcome[A]
  type AP = BootstrapEstimationPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  protected var _confidence: Double = 0.95
  protected var _confidence_interval: ConfidenceInterval = Unconstrained()
  protected var _default_sample_size: Int = 30
  protected var _estimator: Seq[Double] => Double = {
    // by default, use the mean
    ds => ds.sum / ds.length
  }

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence
  def confidence_interval_=(ci: ConfidenceInterval) { _confidence_interval = ci }
  def confidence_interval: ConfidenceInterval = _confidence_interval
  def default_sample_size: Int = _default_sample_size
  def default_sample_size_=(n: Int) { _default_sample_size = n }
  def estimator: Seq[Double] => Double = _estimator
  def estimator_=(fn: Seq[Double] => Double) { _estimator = fn }

  override protected[automan] def getQuestionType = QuestionType.FreeTextQuestion
  override protected[automan] def getOutcome(adapter: AutomanAdapter) : O = {
    val scheduler = new Scheduler(this, adapter)
    val f = Future{
      blocking {
        scheduler.run().asInstanceOf[AA]
      }
    }
    ScalarOutcome(f)
  }

  // private methods
  override private[automan] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new AP(_estimator, _confidence_interval, this)
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
