package edu.umass.cs.automan.core.logging

import java.util.{UUID, Date}

import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

object TestUtil {
  def newThunk[A](question: Question[A], timeout: Int, worker_timeout: Int, cost: BigDecimal, time_delta: Int) = {
    val now = new Date()
    Thunk[A](
      UUID.randomUUID(),
      question,
      timeout,
      worker_timeout,
      cost,
      now,
      SchedulerState.READY,
      from_memo = false,
      None,
      None,
      now
    )
  }
}