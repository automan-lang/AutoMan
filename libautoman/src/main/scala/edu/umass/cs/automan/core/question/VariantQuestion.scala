package edu.umass.cs.automan.core.question
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.mturk.question.MTEstimationQuestion
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractAnswer, Outcome, VariantOutcome}
import edu.umass.cs.automan.core.grammar.{Grammar, QuestionProduction}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.question.EstimationQuestion
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.policy.aggregation.AggregationPolicy
import edu.umass.cs.automan.core.policy.price.{MLEPricePolicy, PricePolicy}
import edu.umass.cs.automan.core.policy.timeout.{DoublingTimeoutPolicy, TimeoutPolicy}
import edu.umass.cs.automan.core.question.confidence.{ConfidenceInterval, UnconstrainedCI}

abstract class VariantQuestion extends Question {
  type QuestionOptionType <: QuestionOption
  //type O = Question#O
  type O = VariantOutcome[A]

//  type A <: Any			// return type of the function (what you get when you call .value)
//  type AA <: AbstractAnswer[A]	// an instance of scheduler
//  type O <: Outcome[A]		// outcome is value returned by the scheduler
//  type AP <: AggregationPolicy	// how to derive a scalar value of type A from the distribution of values
//  type PP <: PricePolicy	// how to determine reward
//  type TP <: TimeoutPolicy	// how long to run the job

  override private[automan] def validation_policy_instance = newQ.validation_policy_instance.asInstanceOf[AP]

  protected var newQ: Question
  protected var _question: QuestionProduction
  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()
  protected var _questions: List[Question] = List[Question]()
  protected var _grammar: Grammar

  // Special variant stuff
  def question: QuestionProduction = _question
  def question_=(q: QuestionProduction) { _question = q }
  def questions: List[Question] = _questions
  def questions_=(q: List[Question]) { _questions = q }
  def grammar: Grammar = _grammar
  def grammar_=(g: Grammar) { _grammar = g }
  def addQuestion(q: Question): List[Question] = {
    _questions = _questions :+ q
    _questions
  }

  // RB Vector stuff
  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QuestionOptionType]

  // Discrete Scalar stuff
  protected var _confidence: Double = 0.95

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  // Estimate stuff
  protected var _confidence_interval: ConfidenceInterval = UnconstrainedCI()
  protected var _default_sample_size: Int = 12
  protected var _estimator: Seq[Double] => Double = {
    // by default, use the mean
    ds => ds.sum / ds.length
  }
  protected var _min_value: Option[Double] = None
  protected var _max_value: Option[Double] = None

  def confidence_interval_=(ci: ConfidenceInterval) { _confidence_interval = ci }
  def confidence_interval: ConfidenceInterval = _confidence_interval
  def default_sample_size: Int = _default_sample_size
  def default_sample_size_=(n: Int) { _default_sample_size = n }
  def estimator: Seq[Double] => Double = _estimator
  def estimator_=(fn: Seq[Double] => Double) { _estimator = fn }
  def max_value: Double = _max_value match {
    case Some(v) => v
    case None => Double.PositiveInfinity
  }
  def max_value_=(max: Double) { _max_value = Some(max) }
  def min_value: Double = _min_value match {
    case Some(v) => v
    case None => Double.NegativeInfinity
  }
  def min_value_=(min: Double) { _min_value = Some(min) }

  override protected[automan] def getQuestionType: QuestionType = QuestionType.VariantQuestion
  //override protected[automan] def getQuestionType(): QuestionType = { question.questionType }

  // Methods
  override private[automan] def init_validation_policy(): Unit = {
    newQ.init_validation_policy()
    //      question.questionType match {
    //        case QuestionType.EstimationQuestion => {
    //          this.asInstanceOf[MTEstimationQuestion].init_validation_policy()
    //        }
    //      }
  }

  override private[automan] def init_price_policy(): Unit = {
    newQ.init_price_policy()
    //      question.questionType match {
    //        case QuestionType.EstimationQuestion => {
    //          this.asInstanceOf[MTEstimationQuestion].init_price_policy()
    //        }
    //      }
  }

  override private[automan] def init_timeout_policy(): Unit = {
    newQ.init_timeout_policy()
    //      question.questionType match {
    //        case QuestionType.EstimationQuestion => {
    //          this.asInstanceOf[MTEstimationQuestion].init_timeout_policy()
    //        }
    //      }
  }

  override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): MockResponse = {
    //newQ.toMockResponse(question_id, response_time, a, worker_id)
          question.questionType match {
            case QuestionType.EstimationQuestion => {
              newQ.asInstanceOf[MTEstimationQuestion].toMockResponse(question_id, response_time, a.asInstanceOf[MTEstimationQuestion#A], worker_id)
            }
          }
  }

  override protected[automan] def prettyPrintAnswer(answer: A): String = {
    //newQ.prettyPrintAnswer(answer.asInstanceOf[A])
    question.questionType match {
      case QuestionType.EstimationQuestion => {
        newQ.asInstanceOf[MTEstimationQuestion].prettyPrintAnswer(answer.asInstanceOf[MTEstimationQuestion#A])
      }
    }
    //      question.questionType match {
    //        case QuestionType.EstimationQuestion => {
    //          this.asInstanceOf[MTEstimationQuestion].prettyPrintAnswer(answer.asInstanceOf[MTEstimationQuestion#A])
    //        }
    //      }
  }

  override protected[automan] def getOutcome(adapter: AutomanAdapter): O = {
    //newQ.getOutcome(adapter)
    VariantOutcome(this, schedulerFuture(adapter))
//    question.questionType match {
//      case QuestionType.EstimationQuestion => {
//        newQ.asInstanceOf[MTEstimationQuestion].getOutcome(adapter)
//      }
//    }
    //VariantOutcome(this, schedulerFuture(adapter))
  }

  override protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter): O = {
    //newQ.composeOutcome(o.asInstanceOf[O], adapter).asInstanceOf[O]
    question.questionType match {
      case QuestionType.EstimationQuestion => {
        newQ.asInstanceOf[MTEstimationQuestion].composeOutcome(o.asInstanceOf[MTEstimationQuestion#O], adapter).asInstanceOf[O]
      }
    }
    //      question.questionType match {
    //        case QuestionType.EstimationQuestion => {
    //          this.asInstanceOf[MTEstimationQuestion].composeOutcome(o.asInstanceOf[MTEstimationQuestion#O], adapter).asInstanceOf[O]
    //        }
    //      }
  }

}
