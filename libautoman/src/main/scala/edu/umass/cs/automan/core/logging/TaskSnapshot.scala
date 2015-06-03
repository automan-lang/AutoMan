package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.scheduler.SchedulerState

case class TaskSnapshot[T](task_id: UUID,
                           question_id: UUID,
                           title: String,
                           text: String,
                           round: Int,
                           timeout_in_s: Int,
                           worker_timeout: Int,
                           cost: BigDecimal,
                           created_at: Date,
                           state: SchedulerState.Value,
                           worker_id: Option[String],
                           answer: Option[T],
                           state_changed_at: Date) {
  def this(tup: (UUID,UUID,String,String,Int,Int,Int,BigDecimal,Date,SchedulerState.Value,Option[String],Option[T],Date)) =
  this(tup._1,tup._2,tup._3,tup._4,tup._5,tup._6,tup._7,tup._8,tup._9,tup._10,tup._11,tup._12,tup._13)
}
