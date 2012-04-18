package edu.umass.cs.automan.core.answer

abstract class Answer(val conf: Option[Double], val worker_id: String) {
  var _confidence = conf
  var custom_info: Option[String] = None
  var paid: Boolean = false
  var over_budget = false
//  var timeout = false
  var final_cost = 0
  // A method that lets the Strategy decide when two answers are the same;
  // all answers have a canonical form, which must be the same whether an
  // answer is or is not for a question dual.
  def comparator: Symbol
  def confidence_=(conf: Double) { _confidence = Some(conf) }
  def confidence = _confidence match { case Some(c) => c; case None => 0.00 }
}
