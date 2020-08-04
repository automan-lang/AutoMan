package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.policy.aggregation.AdversarialPolicy
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy

abstract class CheckboxQuestion extends DiscreteScalarQuestion {
  type A = Set[Symbol]
  type QuestionOptionType <: QuestionOption
  type AP = AdversarialPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = {
    val base = BigInt(2)
    base.pow(options.size)
  }
  def randomized_options: List[QuestionOptionType]

  override protected[automan] def getQuestionType = QuestionType.CheckboxQuestion

  // private methods
  override private[automan] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new AP(this)
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

  override protected[automan] def prettyPrintAnswer(answer: Set[Symbol]): String = {
    var optionMap: Map[Symbol, String] = Map[Symbol, String]() // map option symbols to option text
    for(o <- options) optionMap += (o.question_id -> o.question_text)

    "(" + answer.map(optionMap(_)).mkString(", ") + ")"
  }
}
