package edu.umass.cs.automan.core.logging

object LogType extends Enumeration {
  type LogType = Value
  val STRATEGY,
  SCHEDULER,
  ADAPTER,
  MEMOIZER
  = Value
}
