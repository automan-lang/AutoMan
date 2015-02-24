package edu.umass.cs.automan.core.logging

/**
 * Created by dbarowy on 2/24/15.
 */
object LogType extends Enumeration {
  type LogType = Value
  val STRATEGY,
  SCHEDULER,
  ADAPTER,
  MEMOIZER
  = Value
}
