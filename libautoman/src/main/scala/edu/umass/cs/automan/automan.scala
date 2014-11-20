package edu.umass.cs.automan

import edu.umass.cs.automan.core.AutomanAdapter

object automan {
  def apply[T](a: AutomanAdapter)(block: => T): T = {
//    a.init()    Adapters are initialized upon declaration, for now
    val output = block
    a.close()
    output
  }
}
