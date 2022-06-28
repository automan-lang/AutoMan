package org.automanlang.core.answer

import com.github.tototoshi.csv._
import org.automanlang.core.AutomanAdapter
import org.automanlang.core.question._

import java.io.{File, IOException}
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
  def combineWith(e: EstimationOutcome)(op: Double => Double => Double)(implicit adapter: AutomanAdapter): EstimationOutcome = {
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

case class SurveyOutcome[T](_question: FakeSurvey,
                            override protected[automanlang] val f: Future[AbstractSurveyAnswer[T]])
  extends Outcome[T](_question, f) {
  override def toString: String = {
    answer match {
      case a: SurveyAnswers[T] => a.values.toString
      case a: SurveyIncompleteAnswers[T] => a.values.toString()
      case _: SurveyOverBudgetAnswers[T] => "(no answers, over budget)"
      case _: SurveyNoAnswers[T] => "(no answers)"
      case _ => throw new Exception("SurveyOutcome in unknown state.")
    }
  }

  def saveToCSV(): Unit = {
    try {
      val writer = CSVWriter.open(new File(_question.csv_output + ".final"))

      // CSV header: worker_id, metadata, questions
      writer.writeRow(List("Worker ID", "cost") ::: _question.questions.map(q => q.text))

      // CSV content
      answer match {
        // TODO: Add a cost column (Something to do with Task?)
        case a: SurveyAnswers[T] =>
          val a_typed = a.asInstanceOf[SurveyAnswers[FakeSurvey#A]]

          a_typed.values.foreach(ans => {
            writer.writeRow(List(ans._1, a_typed.metadatas(ans._1).cost) ::: ans._2)
          })

        case _ => throw new Exception("SurveyOutcome is not in ANSWERED state.")
      }

      writer.close()
    } catch {
      case e: IOException =>
        println(s"[ERROR] IOException while trying to open file ${_question.csv_output} for write.")
        return
    }
  }
}
