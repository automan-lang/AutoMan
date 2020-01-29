package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.question._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

sealed abstract class Outcome[T](_question: Question,
                                 protected[automan] val f: Future[AbstractAnswer[T]]) {
  def answer: AbstractAnswer[T] = {
    Await.result(f, Duration.Inf)
  }
  def question: Question = _question
}

case class MultiEstimationOutcome(_question: MultiEstimationQuestion,
                                  override protected[automan] val f: Future[AbstractMultiEstimate])
  extends Outcome[Array[Double]](_question, f)

case class EstimationOutcome(_question: EstimationQuestion,
                             override protected[automan] val f: Future[AbstractEstimate])
  extends Outcome[Double](_question, f) {
  def combineWith(e: EstimationOutcome)(op: Double => Double => Double)(implicit adapter: AutomanAdapter) : EstimationOutcome = {
    // create combination question
    val mq = EstimationMetaQuestion(lhs = this._question, rhs = e._question, op = op)
    // schedule
    adapter.schedule[EstimationMetaQuestion](mq, mqp => Unit)
  }
}

case class ScalarOutcome[T](_question: DiscreteScalarQuestion,
                            override protected[automan] val f: Future[AbstractScalarAnswer[T]])
  extends Outcome[T](_question, f)

case class VectorOutcome[T](_question: VectorQuestion,
                            override protected[automan] val f: Future[AbstractVectorAnswer[T]])
  extends Outcome[T](_question, f)

case class SurveyOutcome(survey: Survey,
                              override protected[automan] val f: Future[AbstractSurveyAnswer])
  extends Outcome[Set[(String,Question#A)]](survey, f)

case class GrammarOutcome[T](_question: GrammarQuestion,
                             override protected[automan] val f: Future[AbstractAnswer[T]]) //TODO is this actually overriding anything?
  extends Outcome[T](_question, f)