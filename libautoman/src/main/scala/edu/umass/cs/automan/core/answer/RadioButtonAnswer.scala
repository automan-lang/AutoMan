package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.RadioButtonAnswerMemo

class RadioButtonAnswer(conf: Option[Double], worker_id: String, val value: Symbol)
  extends ScalarAnswer(conf: Option[Double], worker_id: String) {
  type AnswerValueType = Symbol

  var memo_handle: RadioButtonAnswerMemo = _
  override def comparator = value  // ignore is_dual
  override def final_answer(confidence: Option[Double]) : RadioButtonAnswer = {
    confidence match {
      case Some(c) => new RadioButtonAnswer(Some(c), "aggregated", value)
      case None => throw new AnswerConfidenceMissingException("A final RadioButtonScalarAnswer requires a confidence value.")
    }
  }
  override def toString = value.toString().drop(1)
}
