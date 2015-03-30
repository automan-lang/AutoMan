package edu.umass.cs.automan.adapters.mturk.mock

object HITBackendStatus extends Enumeration {
  type HITBackendStatus = Value
  val RUNNING,
      EXPIRED
  = Value
}
