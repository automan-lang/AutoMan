package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.CheckboxAnswerMemo

class CheckboxAnswer(conf: Option[Double], worker_id: String, val values: Set[Symbol])
  extends ScalarAnswer(conf, worker_id) {
  type AnswerValueType = Set[Symbol]

  var memo_handle: CheckboxAnswerMemo = _
  override def comparator = values
  override def final_answer(confidence: Option[Double]) : CheckboxAnswer = {
    confidence match {
      case Some(c) => new CheckboxAnswer(Some(c), "aggregated", values)
      case None => throw new AnswerConfidenceMissingException("A final CheckboxScalarAnswer requires a confidence value.")
    }
  }
  override def toString = values.map(_.toString()).reduceLeft(_ + ", " + _)
}
