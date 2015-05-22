package edu.umass.cs.automan.adapters.MTurk.mock

object AssignmentStatus extends Enumeration {
  type AssignmentStatus = Value
  val ANSWERED,
      UNANSWERED,
      APPROVED,
      REJECTED
  = Value
}
