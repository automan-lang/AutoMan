package edu.umass.cs.automan.core.answer

import java.util.Calendar
import edu.umass.cs.automan.core.memoizer.AnswerMemo

abstract class Answer(workerId: String) {
  class AnswerConfidenceMissingException(msg: String) extends Exception(msg)
  class AnswerConfidenceSuppliedException(msg: String) extends Exception(msg)
  type AnswerValueType
  type AnswerMemoType <: AnswerMemo

  var custom_info: Option[String] = None
  var paid: Boolean = false
  var memo_handle: AnswerMemoType
  val worker_id: String = workerId
  var _accept_time: Calendar = Calendar.getInstance();
  var _submit_time: Calendar = Calendar.getInstance();
  def accept_time = _accept_time
  def accept_time_=(v: Calendar) { _accept_time = v }
  def comparator: AnswerValueType
  def final_answer(confidence: Option[Double]): Answer
  def sameAs[A <: Answer](answer: A): Boolean = this.comparator == answer.comparator
  def submit_time = _submit_time
  def submit_time_=(v: Calendar) { _submit_time = v }
}
