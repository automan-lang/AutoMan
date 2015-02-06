package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.strategy.DefaultScalarStrategy

abstract class ScalarQuestion[A] extends Question[A] {
  protected var _confidence: Double = 0.95
  type VS = DefaultScalarStrategy[A]

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  override private[automan] def init_strategy(): Unit = {
    _strategy_instance = _strategy match {
      case None => new DefaultScalarStrategy[A](this)
      case Some(strat) => strat.newInstance()
    }
  }
}
