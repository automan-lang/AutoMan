package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.ScalarAnswer
import edu.umass.cs.automan.core.strategy.{DefaultDistributionStrategy, DistributionValidationStrategy}

abstract class DistributionQuestion extends Question {
  type VS = DistributionValidationStrategy[this.type, A, B]
  type A <: ScalarAnswer
  type B = Set[A]

  override val _is_for_distribution = true

  var _num_samples: Int = 30
  // the proportion of workers who will resubmit work
  // that we will have to throw away but pay for anyway.
  val PROP_RETURNING_WORKERS = 0.4

  private[automan] def init_strategy {
    _strategy_instance = _strategy match {
      case None => new DefaultDistributionStrategy[this.type, A, B](this, _num_samples)
      case Some(strat) => strat.newInstance() // TODO: this constructor should be able to take arguments
    }
  }
  override def budget: BigDecimal = _budget match {
    case Some(b) => b
    // the default budget should accommodate at least
    // the number of samples required.
    // this figure assumes that some workers will return
    // so the number is adjusted upward some
    case None => (reward * num_samples) / (1 - PROP_RETURNING_WORKERS)
  }
  def num_samples_=(n: Int) { _num_samples = n }
  def num_samples: Int = _num_samples
}
