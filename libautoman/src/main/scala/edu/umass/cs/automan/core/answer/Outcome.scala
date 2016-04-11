package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.question._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

sealed abstract class Outcome[T](question: Question,
                                 protected[automan] val f: Future[AbstractAnswer[T]]) {
  def answer: AbstractAnswer[T] = {
    Await.result(f, Duration.Inf)
  }
}

case class MultiEstimationOutcome(question: MultiEstimationQuestion,
                                  override protected[automan] val f: Future[AbstractMultiEstimate])
  extends Outcome[Array[Double]](question, f)

case class EstimationOutcome(question: EstimationQuestion,
                             override protected[automan] val f: Future[AbstractEstimate])
  extends Outcome[Double](question, f) {
  def combineWith(e: EstimationOutcome)(op: Double => Double => Double)(implicit adapter: AutomanAdapter) : EstimationOutcome = {
    // create combination question
    val mq = EstimationMetaQuestion(lhs = this.question, rhs = e.question, op = op)
    // schedule
    adapter.schedule[EstimationMetaQuestion](mq, mqp => Unit)
  }
}

case class ScalarOutcome[T](question: DiscreteScalarQuestion,
                            override protected[automan] val f: Future[AbstractScalarAnswer[T]])
  extends Outcome[T](question, f)

case class VectorOutcome[T](question: VectorQuestion,
                            override protected[automan] val f: Future[AbstractVectorAnswer[T]])
  extends Outcome[T](question, f)