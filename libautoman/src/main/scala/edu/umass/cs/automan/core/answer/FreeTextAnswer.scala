package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.FreeTextAnswerMemo

class FreeTextAnswer(conf: Option[Double], worker_id: String, val value: Symbol)
  extends ScalarAnswer(conf, worker_id) {
  type AnswerValueType = Symbol
  type AnswerMemoType = FreeTextAnswerMemo

  var memo_handle: FreeTextAnswerMemo = _
  override def comparator = value
  override def final_answer(confidence: Option[Double]) : FreeTextAnswer = {
    confidence match {
      case Some(c) => new FreeTextAnswer(Some(c), "aggregated", value)
      case None => throw new AnswerConfidenceMissingException("A final FreeTextScalarAnswer requires a confidence value.")
    }
  }
  override def toString = value.toString().drop(1)
}
