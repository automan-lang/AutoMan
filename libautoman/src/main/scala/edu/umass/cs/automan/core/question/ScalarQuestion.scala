package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.ScalarAnswer
import edu.umass.cs.automan.core.strategy.{DefaultScalarStrategy, ScalarValidationStrategy}

abstract class ScalarQuestion extends Question {
  type A <: ScalarAnswer
  type VS = ScalarValidationStrategy[this.type, A, B]

  protected var _confidence: Option[Double] = None
  def confidence: Double = _confidence match { case Some(c) => c; case None => 0.95 }
  def confidence_=(c: Double) { _confidence = Some(c) }
  private[automan] def init_strategy() {
    val s = _strategy match {
      case None => new DefaultScalarStrategy[this.type, A, B](this)
      case Some(strat) => strat.newInstance()
    }
    s.confidence = this.confidence
    s.num_possibilities = this.num_possibilities
    _strategy_instance = s
  }
}
