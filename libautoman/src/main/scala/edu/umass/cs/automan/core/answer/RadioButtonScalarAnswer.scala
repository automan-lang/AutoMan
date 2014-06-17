package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.RadioButtonAnswerMemo

class RadioButtonScalarAnswer(override val conf: Option[Double], override val worker_id: String, val value: Symbol)
  extends ScalarAnswer(conf: Option[Double], worker_id: String) {
  type AnswerValueType = Symbol

  var memo_handle: RadioButtonAnswerMemo = _
  def comparator = value  // ignore is_dual
  override def final_answer(confidence: Option[Double]) : RadioButtonScalarAnswer = {
    confidence match {
      case Some(c) => new RadioButtonScalarAnswer(Some(c), "aggregated", value)
      case None => throw new AnswerConfidenceMissingException("A final RadioButtonScalarAnswer requires a confidence value.")
    }
  }
  override def toString = value.toString()
}
