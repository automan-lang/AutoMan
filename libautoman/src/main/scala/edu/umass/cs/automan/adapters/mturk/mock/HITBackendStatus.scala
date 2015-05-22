package edu.umass.cs.automan.adapters.MTurk.mock

object HITBackendStatus extends Enumeration {
  type HITBackendStatus = Value
  val RUNNING,
      EXPIRED
  = Value
}
