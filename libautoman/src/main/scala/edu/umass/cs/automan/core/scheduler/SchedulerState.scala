package edu.umass.cs.automan.core.scheduler

object SchedulerState extends Enumeration {
  type SchedulerState = Value
  val READY, // OK to execute
      RUNNING, // task has been sent to crowdsourcing backend
      RETRIEVED, // answer has been retrieved from backend
      ACCEPTED, // answer has been paid for
      REJECTED, // answer is incorrect (and will not be paid for)
      PROCESSED, // answer was accepted/rejected in previous execution (for memo-recalled Thunks)
      TIMEOUT, // thunk timed out (reschedule)
      CANCELLED
  = Value
}

import SchedulerState._