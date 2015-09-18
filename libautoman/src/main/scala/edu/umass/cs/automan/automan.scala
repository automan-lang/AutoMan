package edu.umass.cs.automan

import edu.umass.cs.automan.core.AutomanAdapter

object automan {
  def apply[T](a: AutomanAdapter, test_mode: Boolean = false)(block: => T): T = {
    if (test_mode) a.clearMemoDB()
    val output = block
    a.close()
    if (test_mode) a.clearMemoDB()
    output
  }
}
