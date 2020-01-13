package edu.umass.cs.automan.core.question
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractSurveyAnswer, Answers, SurveyOutcome}
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.policy.aggregation.{SimpleSurveyPolicy, SurveyPolicy, VectorPolicy}
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy

// abstract Survey class (will implement in adapters)
abstract class Survey extends Question {
  type A = Array[Any] // the return type (.value)
  type AA <: AbstractSurveyAnswer // TODO should these be overriding?
  type O <: SurveyOutcome
  type AP = SimpleSurveyPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  private var _sample_size: Int = 30 // TODO necessary?
  def sample_size_=(n: Int) { _sample_size = n }
  def sample_size : Int = _sample_size

  // hash of something in Q so memoizer knows when Qs are the same; def at implementation level
  //override def memo_hash: String = ???

  // TODO: AP SurveyPolicy? (Will just accept answers for now; later may reject/mark outliers)
  override private[automan] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new AP(this) // why isn't this working?
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override private[automan] def init_price_policy(): Unit = {
    _price_policy_instance = _price_policy match {
      case None => new PP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override private[automan] def init_timeout_policy(): Unit = {
    _timeout_policy_instance = _timeout_policy match {
      case None => new TP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  protected[automan] def getOutcome(adapter: AutomanAdapter): O = {
    SurveyOutcome(this, schedulerFuture(adapter)).asInstanceOf[O]
  }

  // TODO: Do we need?
//  protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter): O = {
//    // unwrap future from previous Outcome
//    val f = o.f map {
//      case Answers(values, _, id, dist) =>
//        Answers(
//          values,
//          BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
//          id,
//          dist
//        )
//      case _ => startScheduler(adapter)
//
//    }
//    SurveyOutcome(this, f).asInstanceOf[O]
//  }

  //TODO: What will this look like for survey?
  //protected[automan] def getQuestionType: QuestionType = ???
}
