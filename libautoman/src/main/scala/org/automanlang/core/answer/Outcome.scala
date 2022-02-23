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
  extends Outcome[Array[Double]](_question, f) {
  override def toString: String = {
    answer match {
      case e: MultiEstimate => e.values.toString
      case e: LowConfidenceMultiEstimate => e.values.toString
      case _: OverBudgetMultiEstimate => "(no estimates, over budget)"
      case _: NoMultiEstimate => "(no estimates)"
      case _ => throw new Exception("MultiEstimationOutcome in unknown state.")
    }
  }
}

case class EstimationOutcome(_question: EstimationQuestion,
                             override protected[automanlang] val f: Future[AbstractEstimate])
  extends Outcome[Double](_question, f) {
  def combineWith(e: EstimationOutcome)(op: Double => Double => Double)(implicit adapter: AutomanAdapter) : EstimationOutcome = {
    // create combination question
    val mq = EstimationMetaQuestion(lhs = this._question, rhs = e._question, op = op)
    // schedule
    adapter.schedule[EstimationMetaQuestion](mq, mqp => Unit)
  }
  override def toString: String = {
    answer match {
      case e: Estimate => e.value.toString
      case e: LowConfidenceEstimate => e.value.toString
      case _: OverBudgetEstimate => "(no estimate, over budget)"
      case _: NoEstimate => "(no estimate)"
      case _ => throw new Exception("EstimationOutcome in unknown state.")
    }
  }
}

case class ScalarOutcome[T](_question: DiscreteScalarQuestion,
                            override protected[automanlang] val f: Future[AbstractScalarAnswer[T]])
  extends Outcome[T](_question, f) {
  override def toString: String = {
    answer match {
      case a: Answer[T] => a.value.toString
      case a: LowConfidenceAnswer[T] => a.value.toString
      case _: OverBudgetAnswer[T] => "(no answer, over budget)"
      case _: NoAnswer[T] => "(no answer)"
      case _ => throw new Exception("ScalarOutcome in unknown state.")
    }
  }
}

case class VectorOutcome[T](_question: VectorQuestion,
                            override protected[automanlang] val f: Future[AbstractVectorAnswer[T]])
  extends Outcome[T](_question, f) {
  override def toString: String = {
    answer match {
      case a: Answers[T] => a.values.toString
      case a: IncompleteAnswers[T] => a.values.toString()
      case _: OverBudgetAnswers[T] => "(no answers, over budget)"
      case _: NoAnswers[T] => "(no answers)"
      case _ => throw new Exception("VectorOutcome in unknown state.")
    }
  }
}


case class MixedOutcome[T](_question: MixedQuestion,
                           override protected[automanlang] val f: Future[AbstractMixedAnswer[T]])
  extends Outcome[T](_question, f) {
  override def toString: String = {
    answer match {
      case a: Answers[T] => a.values.toString
      case a: IncompleteAnswers[T] => a.values.toString()
      case _: OverBudgetAnswers[T] => "(no answers, over budget)"
      case _: NoAnswers[T] => "(no answers)"
      case _ => throw new Exception("MixedOutcome in unknown state.")
    }
  }
}