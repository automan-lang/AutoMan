package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{ScalarOutcome, AbstractScalarAnswer}
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.strategy.DefaultScalarStrategy
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class ScalarQuestion extends Question {
  protected var _confidence: Double = 0.95
  type AA <: AbstractScalarAnswer[A]
  type VS = DefaultScalarStrategy
  type O <: ScalarOutcome[A]

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  override private[automan] def init_strategy(): Unit = {
    _strategy_instance = _strategy match {
      case None => new DefaultScalarStrategy(this)
      case Some(strat) => strat.newInstance()
    }
  }

  override protected[automan] def getOutcome(adapter: AutomanAdapter, memo: Memo, poll_interval_in_s: Int): O
}
