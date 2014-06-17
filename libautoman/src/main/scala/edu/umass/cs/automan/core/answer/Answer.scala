package edu.umass.cs.automan.core.answer

abstract class Answer {
  class AnswerConfidenceMissingException(msg: String) extends Exception(msg)
  class AnswerConfidenceSuppliedException(msg: String) extends Exception(msg)

  var custom_info: Option[String] = None
  var final_cost: BigDecimal = 0
  var over_budget = false
  var paid: Boolean = false
  def comparator: Symbol
  def final_answer(confidence: Option[Double]): Answer
}
