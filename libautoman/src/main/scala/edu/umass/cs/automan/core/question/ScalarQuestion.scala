package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{ScalarOutcome, AbstractScalarAnswer}
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.strategy.DefaultScalarStrategy
import edu.umass.cs.automan.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class ScalarQuestion extends Question {
  type AA <: AbstractScalarAnswer[A]
  type VS = DefaultScalarStrategy
  type O <: ScalarOutcome[A]
  
  protected var _confidence: Double = 0.95

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  override private[automan] def init_strategy(): Unit = {
    _strategy_instance = _strategy match {
      case None => new DefaultScalarStrategy(this)
      case Some(strat) => strat.newInstance()
    }
  }
}
