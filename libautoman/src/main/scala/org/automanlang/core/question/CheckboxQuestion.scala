package org.automanlang.core.question

import org.automanlang.core.info.QuestionType
import org.automanlang.core.policy.aggregation.AdversarialPolicy
import org.automanlang.core.policy.price.MLEPricePolicy
import org.automanlang.core.policy.timeout.DoublingTimeoutPolicy

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

  override protected[automanlang] def getQuestionType = QuestionType.CheckboxQuestion

  // private methods
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

  override protected[automanlang] def prettyPrintAnswer(answer: Set[Symbol]): String = {
    var optionMap: Map[Symbol, String] = Map[Symbol, String]() // map option symbols to option text
    for(o <- options) optionMap += (o.question_id -> o.question_text)

    "(" + answer.map(optionMap(_)).mkString(", ") + ")"
  }

  // method that returns an array of the possible responses
  def return_response_possibilities() : Array[String] = {

    // flatten list of combinations
    val textOptions = _options.map(o => o.question_id.name)

    // get all combinations
    val combinations = textOptions.toSet[String].subsets.map(_.toList).toList

    // flatten into string and turn into array
    var arr: Array[String] = Array()

    combinations.foreach(c => {

      val sortedC = c.sorted

      var word = ""
      var length = sortedC.length
      var index = 1

      sortedC.foreach(part => {
        word = word + part
        if (index < length) {
          word = word + ", "
        }
        index = index + 1
      })

      arr = arr :+ word

    })

//    arr.foreach(r => {
//      println(r)
//    })
    arr


  }


}
