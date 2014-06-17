package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.CheckboxAnswerMemo

class CheckboxScalarAnswer(override val conf: Option[Double], override val worker_id: String, val values: Set[Symbol])
  extends ScalarAnswer(conf: Option[Double], worker_id: String) {
  var memo_handle: CheckboxAnswerMemo = _
  def comparator = Symbol(values.toList.map(_.toString.drop(1)).sortBy{a => a}.reduceLeft{_ + _})
  override def final_answer(confidence: Option[Double]) : CheckboxScalarAnswer = {
    confidence match {
      case Some(c) => new CheckboxScalarAnswer(Some(c), "aggregated", values)
      case None => throw new AnswerConfidenceMissingException("A final CheckboxScalarAnswer requires a confidence value.")
    }
  }
  override def toString = values.map(_.toString()).reduceLeft(_ + ", " + _)
}
