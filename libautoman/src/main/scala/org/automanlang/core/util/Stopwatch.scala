package org.automanlang.core.util

case class Time[A](result: A, duration_ms: Long)

object Stopwatch {
  def apply[A](fn: => A) : Time[A] = {
    val start_time = System.currentTimeMillis()
    val result = fn
    val end_time = System.currentTimeMillis()
    Time(result, end_time - start_time)
  }
}
