package edu.umass.cs.automan.core.answer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

sealed abstract class Outcome[T](f: Future[AbstractAnswer[T]]) {
  def answer: AbstractAnswer[T] = {
    Await.result(f, Duration.Inf)
  }
}

case class ScalarOutcome[T](f: Future[AbstractScalarAnswer[T]]) extends Outcome[T](f)

case class DistributionOutcome[T](f: Future[AbstractVectorAnswer[T]]) extends Outcome[T](f)