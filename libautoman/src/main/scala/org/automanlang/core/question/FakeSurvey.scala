package org.automanlang.core.question

import org.automanlang.core.AutomanAdapter
import org.automanlang.core.answer.{AbstractSurveyAnswer, SurveyAnswers, SurveyOutcome}
import org.automanlang.core.info.QuestionType
import org.automanlang.core.info.QuestionType.QuestionType
import org.automanlang.core.mock.MockResponse
import org.automanlang.core.policy.aggregation.{AdversarialPolicy, SimpleSurveyVectorPolicy, SurveyPolicy}
import org.automanlang.core.policy.price.{FixedPricePolicy, MLEPricePolicy}
import org.automanlang.core.policy.timeout.{DoublingTimeoutPolicy, FixedTimeoutPolicy}

import java.util.{Date, UUID}
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global

abstract class FakeSurvey extends Question {
  //  perhaps AA = AbstractVectorAnswer[AbstractAnswer[Any]] ???
  override type A = List[Any] // TODO: better type for returned answers
  override type AA = AbstractSurveyAnswer[A]
  override type O = SurveyOutcome[A]

  // TODO: New policies need to be added
  override type AP = SurveyPolicy // Answer Policy
  override type PP = FixedPricePolicy // Price Policy
//  override type TP = DoublingTimeoutPolicy // Timeout Policy
  override type TP = FixedTimeoutPolicy // Timeout Policy

  private var _sample_size: Int = 30

  def sample_size_=(n: Int): Unit = { _sample_size = n }
  def sample_size : Int = _sample_size

  // the sample size that we should collect so we can give users at least
  // _sample_size sized responses that are unlikely to be random, based on our
  // a priori noise_percentage
  def sample_collect_size: Int = {
    (sample_size / (1-noise_percentage)).toInt
  }

  // Cohen's d threshold, used to determine if we have gathered enough responses
  private var _d_threshold: Double = 12
  def d_threshold_=(t: Double): Unit = { _d_threshold = t }
  def d_threshold: Double = _d_threshold

  // noise percentage, used to label responses as likely noise or not
  private var _noise_percentage: Double = 0.2
  def noise_percentage_=(t: Double): Unit = {
    if (t < 0 || t > 1) {
      throw new IllegalArgumentException("noise_percentage must be between 0 and 1")
    }
    _noise_percentage = t
  }
  def noise_percentage: Double = _noise_percentage


  override private[automanlang] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new AP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override private[automanlang] def init_price_policy(): Unit = {
    _price_policy_instance = _price_policy match {
      case None => new PP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override private[automanlang] def init_timeout_policy(): Unit = {
    _timeout_policy_instance = _timeout_policy match {
      case None => new TP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  // list of questions
  type Q <: Question // should be overridden by an adapter-specific question
  private var _questions: List[Q] = List()

  def questions: List[Q] = _questions

  def questions_=(newQs: List[Any]): Unit = _questions = newQs.asInstanceOf[List[Q]] // ways to work around this ugly typecasting?

  // list of candidates for mad-libs styled syntaxs
  private var _words_candidates: ListMap[String, Array[String]] = ListMap()
  def words_candidates: ListMap[String, Array[String]] = _words_candidates
  def words_candidates_=(new_c: ListMap[String, Array[String]]): Unit = {
    _words_candidates = new_c
  }

  // output_var => (input_var, choice)
  // where choice is a Map of [input_var_value => output_var_value] pairs
  private var _functions: ListMap[String, (String, Map[String, String])] = ListMap()

  def functions: ListMap[String, (String, Map[String, String])] = _functions

  def functions_=(new_f: ListMap[String, (String, Map[String, String])]): Unit = {
    _functions = new_f
  }


  override protected[automanlang] def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): MockResponse = ???

  override protected[automanlang] def prettyPrintAnswer(answer: A): String = {
    // TODO: possibly call questions' prettyPrintAnswer? However type "Any"
    // cannot be passed in and explicit type casting is not possible
    answer.mkString(" ")
  }

  override protected[automanlang] def getOutcome(adapter: AutomanAdapter): O = {
    SurveyOutcome(this, schedulerFuture(adapter))
  }

  override protected[automanlang] def composeOutcome(o: O, adapter: AutomanAdapter): O = {
    // unwrap future from previous Outcome
    val f = o.f map {
      case SurveyAnswers(values, metadatas, _, id, dist) =>
        SurveyAnswers(
          values,
          metadatas,
          BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
          id,
          dist
        )
      case _ => startScheduler(adapter)
    }
    SurveyOutcome(this, f).asInstanceOf[O]
  }

  override protected[automanlang] def getQuestionType: QuestionType = QuestionType.Survey
}
