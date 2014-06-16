package edu.umass.cs.automan.core.answer

abstract class Answer {
  var custom_info: Option[String] = None
  var final_cost: BigDecimal = 0
  var over_budget = false
  var paid: Boolean = false
  def comparator: Symbol
}
