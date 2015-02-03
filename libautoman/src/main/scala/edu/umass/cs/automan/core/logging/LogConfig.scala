package edu.umass.cs.automan.core.logging

object LogConfig extends Enumeration {
  type LogConfig = Value
  val NO_LOGGING,         // don't log anything
      LOG,                // log trace only
      LOG_MEMOIZE,        // log trace and use for memoization
      LOG_VERBOSE,        // log trace and output debug information
      LOG_MEMOIZE_VERBOSE // log trace, use for memoization, and output debug information
      = Value
}