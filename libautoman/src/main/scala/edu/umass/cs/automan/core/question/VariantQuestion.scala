package edu.umass.cs.automan.core.question
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractAnswer, Outcome}
import edu.umass.cs.automan.core.grammar.QuestionProduction
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.question.{EstimationQuestion}
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.policy.aggregation.AggregationPolicy
import edu.umass.cs.automan.core.policy.price.{MLEPricePolicy, PricePolicy}
import edu.umass.cs.automan.core.policy.timeout.{DoublingTimeoutPolicy, TimeoutPolicy}

abstract class VariantQuestion extends Question {
  var question: QuestionProduction

//  type A = _
//  type AA = AbstractAnswer[_]
//  type O = Outcome[_]
//  type AP = AggregationPolicy
//  type PP = MLEPricePolicy
//  type TP = DoublingTimeoutPolicy

//  type A <: Any			// return type of the function (what you get when you call .value)
//  type AA <: AbstractAnswer[A]	// an instance of scheduler
//  type O <: Outcome[A]		// outcome is value returned by the scheduler
//  type AP <: AggregationPolicy	// how to derive a scalar value of type A from the distribution of values
//  type PP <: PricePolicy	// how to determine reward
//  type TP <: TimeoutPolicy	// how long to run the job

  override def memo_hash: String = ???

  override private[automan] def init_validation_policy(): Unit = {
    question match {
      case QuestionType.EstimationQuestion => {
        this.asInstanceOf[EstimationQuestion].init_validation_policy()
      }
    }

//    val qt: QuestionType = question.questionType
//    qt.toString match {
//      case "EstimationQuestion" => {
//        Question.EstimationQuestion.init_validation_policy()
//      }
//    }
  }

  override private[automan] def init_price_policy(): Unit = {
    question match {
      case QuestionType.EstimationQuestion => {
        this.asInstanceOf[EstimationQuestion].init_price_policy()
      }
    }
  }

  override private[automan] def init_timeout_policy(): Unit = {
    question match {
      case QuestionType.EstimationQuestion => {
        this.asInstanceOf[EstimationQuestion].init_timeout_policy()
      }
    }
  }

  override protected[automan] def getQuestionType(): QuestionType = { question.questionType }

  override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): MockResponse = {
    question match {
      case QuestionType.EstimationQuestion => {
        this.asInstanceOf[EstimationQuestion].toMockResponse(question_id, response_time, a.asInstanceOf[EstimationQuestion#A], worker_id)
      }
    }
  }

  override protected[automan] def prettyPrintAnswer(answer: A): String = {
    question match {
      case QuestionType.EstimationQuestion => {
        this.asInstanceOf[EstimationQuestion].prettyPrintAnswer(answer.asInstanceOf[EstimationQuestion#A])
      }
    }
  }

  override protected[automan] def getOutcome(adapter: AutomanAdapter): O = {
    question match {
      case QuestionType.EstimationQuestion => {
        this.asInstanceOf[EstimationQuestion].getOutcome(adapter).asInstanceOf[O]
      }
    }
  }

  override protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter): O = {
    question match {
      case QuestionType.EstimationQuestion => {
        this.asInstanceOf[EstimationQuestion].composeOutcome(o.asInstanceOf[EstimationQuestion#O], adapter).asInstanceOf[O]
      }
    }
  }

}
