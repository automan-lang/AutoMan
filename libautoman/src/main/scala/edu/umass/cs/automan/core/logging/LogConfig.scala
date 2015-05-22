package edu.umass.cs.automan.core.logging

object LogConfig extends Enumeration {
  type LogConfig = Value
  val NO_LOGGING,           // don't log anything
      TRACE,                // log trace only
      TRACE_MEMOIZE,        // log trace and use for memoization
      TRACE_VERBOSE,        // log trace and output debug information
      TRACE_MEMOIZE_VERBOSE // log trace, use for memoization, and output debug information
      = Value
}
