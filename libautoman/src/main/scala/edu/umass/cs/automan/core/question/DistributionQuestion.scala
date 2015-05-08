package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.strategy.DefaultDistributionStrategy

abstract class DistributionQuestion extends Question {
  type AA <: AbstractDistributionAnswer[A]
  type VS = DefaultDistributionStrategy
  type O <: DistributionOutcome[A]

  private var _sample_size: Int = 30

  def sample_size_=(n: Int) { _sample_size = n }
  def sample_size : Int = _sample_size

  override private[automan] def init_strategy(): Unit = {
    _strategy_instance = _strategy match {
      case None => new DefaultDistributionStrategy(this)
      case Some(strat) => strat.newInstance()
    }
  }
}
