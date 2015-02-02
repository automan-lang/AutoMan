package edu.umass.cs.automan.core.scheduler

import scala.slick.driver.DerbyDriver.simple._

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

  // bidirectional map for converting to/from Derby
  // datatypes for serialization
  val mapper =
    MappedColumnType.base[SchedulerState, Int](
      {
        case SchedulerState.ACCEPTED => 0
        case SchedulerState.CANCELLED => 1
        case SchedulerState.PROCESSED => 2
        case SchedulerState.READY => 3
        case SchedulerState.REJECTED => 4
        case SchedulerState.RETRIEVED => 5
        case SchedulerState.RUNNING => 6
        case SchedulerState.TIMEOUT => 7
      },
      {
        case 0 => SchedulerState.ACCEPTED
        case 1 => SchedulerState.CANCELLED
        case 2 => SchedulerState.PROCESSED
        case 3 => SchedulerState.READY
        case 4 => SchedulerState.REJECTED
        case 5 => SchedulerState.RETRIEVED
        case 6 => SchedulerState.RUNNING
        case 7 => SchedulerState.TIMEOUT
      }
    )
}

import SchedulerState._

