package edu.umass.cs.automan.core.answer

abstract class Answer(workerId: String) {
  class AnswerConfidenceMissingException(msg: String) extends Exception(msg)
  class AnswerConfidenceSuppliedException(msg: String) extends Exception(msg)
  type AnswerValueType

  var custom_info: Option[String] = None
  var paid: Boolean = false
  val worker_id: String = workerId
  def comparator: AnswerValueType
  def final_answer(confidence: Option[Double]): Answer
  def sameAs[A <: Answer](answer: A): Boolean = this.comparator == answer.comparator
}
