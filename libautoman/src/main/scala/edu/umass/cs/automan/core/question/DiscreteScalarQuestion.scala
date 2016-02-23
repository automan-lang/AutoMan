package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{Answer, ScalarOutcome, AbstractScalarAnswer}
import scala.concurrent.ExecutionContext.Implicits.global

abstract class DiscreteScalarQuestion extends Question {
  type AA = AbstractScalarAnswer[A]
  type O = ScalarOutcome[A]

  protected var _confidence: Double = 0.95

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  def num_possibilities: BigInt

  protected[automan] def getOutcome(adapter: AutomanAdapter) : O = {
    ScalarOutcome(schedulerFuture(adapter))
  }
  protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter) : O = {
    // unwrap future from previous Outcome
    val f = o.f map {
      case Answer(value, cost, conf, id) =>
        if (this.confidence <= conf) {
          Answer(
            value,
            BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
            conf,
            id
          )
        } else {
          startScheduler(adapter)
        }
      case _ => startScheduler(adapter)
    }
    ScalarOutcome(f)
  }
}
