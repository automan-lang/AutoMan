package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.ScalarAnswer
import edu.umass.cs.automan.core.strategy.{DefaultDistributionStrategy, DistributionValidationStrategy}

abstract class DistributionQuestion extends Question {
  type VS = DistributionValidationStrategy[this.type, A, B]
  type A <: ScalarAnswer
  type B = Set[A]

  var _num_samples: Int = 30

  def distribution: Set[A]
  private[automan] def init_strategy {
    _strategy_instance = _strategy match {
      case None => new DefaultDistributionStrategy[this.type, A, B](this, _num_samples)
      case Some(strat) => strat.newInstance() // TODO: this constructor should be able to take arguments
    }
  }
  def num_samples_=(n: Int) { _num_samples = n }
  def num_samples: Int = _num_samples
}
