package org.automanlang.core.answer

import org.automanlang.core.AutomanAdapter
import org.automanlang.core.question._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

sealed abstract class Outcome[T](_question: Question,
                                 protected[automanlang] val f: Future[AbstractAnswer[T]]) {
  def answer: AbstractAnswer[T] = {
    Await.result(f, Duration.Inf)
  }
  def question: Question = _question
}

case class MultiEstimationOutcome(_question: MultiEstimationQuestion,
                                  override protected[automanlang] val f: Future[AbstractMultiEstimate])
  extends Outcome[Array[Double]](_question, f)

case class EstimationOutcome(_question: EstimationQuestion,
                             override protected[automanlang] val f: Future[AbstractEstimate])
  extends Outcome[Double](_question, f) {
  def combineWith(e: EstimationOutcome)(op: Double => Double => Double)(implicit adapter: AutomanAdapter) : EstimationOutcome = {
    // create combination question
    val mq = EstimationMetaQuestion(lhs = this._question, rhs = e._question, op = op)
    // schedule
    adapter.schedule[EstimationMetaQuestion](mq, mqp => Unit)
  }
}

case class ScalarOutcome[T](_question: DiscreteScalarQuestion,
                            override protected[automanlang] val f: Future[AbstractScalarAnswer[T]])
  extends Outcome[T](_question, f)

case class VectorOutcome[T](_question: VectorQuestion,
                            override protected[automanlang] val f: Future[AbstractVectorAnswer[T]])
  extends Outcome[T](_question, f)