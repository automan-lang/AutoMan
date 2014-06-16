package edu.umass.cs.automan.core.answer

abstract class ScalarAnswer(val conf: Option[Double], val worker_id: String)
  extends Answer {

  var _confidence = conf
  def confidence_=(conf: Double) { _confidence = Some(conf) }
  def confidence = _confidence match { case Some(c) => c; case None => 0.00 }
  // A method that lets the Strategy decide when two answers are the same;
  // all answers have a canonical form, which must be the same whether an
  // answer is or is not for a question dual.
}