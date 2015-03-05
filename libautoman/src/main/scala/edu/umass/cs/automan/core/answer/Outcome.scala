package edu.umass.cs.automan.core.answer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract class AbstractAnswer[T](val value: T, val cost: BigDecimal)

abstract class Outcome[T](f: Future[AbstractAnswer[T]]) {
  def answer: AbstractAnswer[T] = {
    Await.result(f, Duration.Inf)
  }
}

sealed abstract class AbstractScalarAnswer[T](value: T, cost: BigDecimal, confidence: Double)
  extends AbstractAnswer[T](value, cost)
case class ScalarAnswer[T](override val value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](value, cost, confidence)
case class ScalarOverBudget[T](override val value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](value, cost, confidence)

case class ScalarOutcome[T](f: Future[AbstractScalarAnswer[T]]) extends Outcome[T](f)
