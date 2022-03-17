package org.automanlang.core.question
import org.automanlang.core.AutomanAdapter
import org.automanlang.core.answer.{AbstractAnswer, ScalarOutcome}
import org.automanlang.core.info.QuestionType
import org.automanlang.core.info.QuestionType.QuestionType
import org.automanlang.core.mock.MockResponse
import org.automanlang.core.policy.aggregation.AdversarialPolicy
import org.automanlang.core.policy.price.MLEPricePolicy
import org.automanlang.core.policy.timeout.DoublingTimeoutPolicy

import java.util.{Date, UUID}

abstract class FakeSurvey extends Question {
  //  perhaps AA = AbstractVectorAnswer[AbstractAnswer[Any]] ???
  override type A = List[Any]
  override type AA = AbstractAnswer[A]
  override type O = ScalarOutcome[A]

  // TODO: New policies need to be added
  override type AP = AdversarialPolicy
  override type PP = MLEPricePolicy
  override type TP = DoublingTimeoutPolicy

  override private[automanlang] def init_validation_policy(): Unit = ???
  override private[automanlang] def init_price_policy(): Unit = ???
  override private[automanlang] def init_timeout_policy(): Unit = ???

  // list of questions
  type Q <: Any  // should be overridden by an adapter-specific question
  private var _questions: List[Q] = List()
  def questions: List[Q] = _questions
  def questions_=(newQs: List[Any]): Unit = _questions = newQs.asInstanceOf[List[Q]]  // ways to work around this ugly typecasting?


  override protected[automanlang] def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): MockResponse = ???

  override protected[automanlang] def prettyPrintAnswer(answer: A): String = ???

  override protected[automanlang] def getOutcome(adapter: AutomanAdapter): O = ???

  override protected[automanlang] def composeOutcome(o: O, adapter: AutomanAdapter): O = ???

  override protected[automanlang] def getQuestionType: QuestionType = QuestionType.Survey
}
