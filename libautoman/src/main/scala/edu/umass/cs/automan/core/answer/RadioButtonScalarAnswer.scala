package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.RadioButtonAnswerMemo

class RadioButtonScalarAnswer(override val conf: Option[Double], override val worker_id: String, val value: Symbol)
  extends ScalarAnswer(conf: Option[Double], worker_id: String) {
  var memo_handle: RadioButtonAnswerMemo = _
  def comparator = value  // ignore is_dual
  override def toString = value.toString()
}
