package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.memoizer.FreeTextAnswerMemo

class FreeTextAnswer(override val conf: Option[Double], override val worker_id: String, val value: Symbol)
  extends Answer(conf: Option[Double], worker_id: String) {
  var memo_handle: FreeTextAnswerMemo = _
  def comparator = value
  override def toString = value.toString()
}
