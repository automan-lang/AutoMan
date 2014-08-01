package edu.umass.cs.automan.core.answer

abstract class ScalarAnswer(conf: Option[Double], worker_id: String)
  extends Answer(worker_id) {

  var _confidence = conf
  def confidence_=(conf: Double) { _confidence = Some(conf) }
  def confidence = _confidence match { case Some(c) => c; case None => 0.00 }
  override def final_answer(confidence: Option[Double]): ScalarAnswer
}