package edu.umass.cs.automan.core.util

// Mostly taken from
// http://stackoverflow.com/questions/7930814/whats-the-scala-way-to-implement-a-retry-able-call-like-this-one

object retry {
  @annotation.tailrec
  def apply[T](n: Int)(fn: => T): T = {
    val r = try { Some(fn) } catch { case e: Exception if n > 1 => None }
    r match {
      case Some(x) => x
      case None => retry(n - 1)(fn)
    }
  }
}