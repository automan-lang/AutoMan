package edu.umass.cs.automan.core.question
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractAnswer, Answer, GrammarOutcome, Outcome, ScalarOutcome}
import edu.umass.cs.automan.core.grammar.{Grammar, Production, Ranking, Scope}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.policy.aggregation.AggregationPolicy
import edu.umass.cs.automan.core.policy.price.{MLEPricePolicy, PricePolicy}
import edu.umass.cs.automan.core.policy.timeout.{DoublingTimeoutPolicy, TimeoutPolicy}

abstract class GrammarQuestion extends Question {
//  type A <: Any			// return type of the function (what you get when you call .value)
//  type AA <: AbstractAnswer[A]	// an instance of scheduler
  type O = GrammarOutcome[A]		// outcome is value returned by the scheduler
//  type AP <: AggregationPolicy	// how to derive a scalar value of type A from the distribution of values
//  type PP <: PricePolicy	// how to determine reward
//  type TP <: TimeoutPolicy	// how long to run the job
//  type PP = MLEPricePolicy
//  type TP = DoublingTimeoutPolicy

  protected var _grammar: Grammar = Grammar(Map[String, Production](), "")
  protected var _scope: Scope = new Scope(grammar, 0)
  protected var _confidence: Double = 0.95
  protected var _bases: Array[Int] = Array[Int]() // represents the number of possibilities for each variable TODO: can we generate?
  //protected var _rank: Ranking = new Ranking

  protected var question: Question

  // getters and setters
  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  def grammar_=(g: Grammar) { _grammar = g }
  def grammar: Grammar = _grammar

  def scope_=(s: Scope) { _scope = s }
  def scope: Scope = _scope

  def bases_=(b: Array[Int]) { _bases = b }
  def bases: Array[Int] = _bases

  def num_possibilities: BigInt

  def generateQuestion(vals: Array[Int], qType: QuestionType): Question
//  = {
//    val teGenInt: Int = Ranking.rank(vals,bases)
//  }

  override def memo_hash: String = question.memo_hash

  // TODO: AP SurveyPolicy? (Will just accept answers for now; later may reject/mark outliers)
  override private[automan] def init_validation_policy(): Unit = {
    question.init_validation_policy()
//    _validation_policy_instance = _validation_policy match {
//      case None => new AP(this)
//      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
//    }
  }

  override private[automan] def init_price_policy(): Unit = {
    question.init_price_policy()
//    _price_policy_instance = _price_policy match {
//      case None => new PP(this)
//      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
//    }
  }

  override private[automan] def init_timeout_policy(): Unit = {
    question.init_timeout_policy()
//    _timeout_policy_instance = _timeout_policy match {
//      case None => new TP(this)
//      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
//    }
  }


  protected[automan] def getOutcome(adapter: AutomanAdapter): O = {
    GrammarOutcome(this, schedulerFuture(adapter))
  }

  protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter) : O = {
    // unwrap future from previous Outcome
    val f = o.f map {
      case Answer(value, cost, conf, id, dist) =>
        if (this.confidence <= conf) {
          Answer(
            value,
            BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
            conf,
            id,
            dist
          )
        } else {
          startScheduler(adapter)
        }
      case _ => startScheduler(adapter)
    }
    GrammarOutcome(this, f)
  }

  override protected[automan] def getQuestionType: QuestionType = QuestionType.GrammarQuestion
}
