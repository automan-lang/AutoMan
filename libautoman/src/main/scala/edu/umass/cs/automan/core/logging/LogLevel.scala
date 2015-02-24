package edu.umass.cs.automan.core.logging

/**
 * Created by dbarowy on 2/24/15.
 */
object LogLevel extends Enumeration {
  type LogLevel = Value
  val INFO,
  WARN,
  FATAL
  = Value
}
