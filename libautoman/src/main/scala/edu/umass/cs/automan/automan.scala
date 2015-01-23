package edu.umass.cs.automan

import edu.umass.cs.automan.core.AutomanAdapter

object automan {
  def apply[T](a: AutomanAdapter)(block: => T): T = {
    val output = block
    a.close()
    output
  }
}
