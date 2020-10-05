package org.automanlang.adapters.mturk.mock

object AssignmentStatus extends Enumeration {
  type AssignmentStatus = Value
  val ANSWERED,
      UNANSWERED,
      APPROVED,
      REJECTED
  = Value
}
