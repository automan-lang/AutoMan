package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.CheckboxAnswerMemo

class CheckboxAnswer(override val conf: Option[Double], override val worker_id: String, val values: Set[Symbol]) extends Answer(conf: Option[Double], worker_id: String) {
  var memo_handle: CheckboxAnswerMemo = _
  def comparator = Symbol(values.toList.map(_.toString.drop(1)).sortBy{a => a}.reduceLeft{_ + _})
  override def toString = values.map(_.toString()).reduceLeft(_ + ", " + _)
}
