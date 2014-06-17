package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.FreeTextAnswerMemo

class FreeTextScalarAnswer(override val conf: Option[Double], override val worker_id: String, val value: Symbol)
  extends ScalarAnswer(conf: Option[Double], worker_id: String) {
  type AnswerValueType = Symbol

  var memo_handle: FreeTextAnswerMemo = _
  def comparator = value
  override def final_answer(confidence: Option[Double]) : FreeTextScalarAnswer = {
    confidence match {
      case Some(c) => new FreeTextScalarAnswer(Some(c), "aggregated", value)
      case None => throw new AnswerConfidenceMissingException("A final FreeTextScalarAnswer requires a confidence value.")
    }
  }
  override def toString = value.toString()
}
