package org.automanlang.core.logging.tables

import java.util.UUID
import org.automanlang.core.scheduler.SchedulerState
import org.automanlang.core.scheduler.SchedulerState.SchedulerState
import scala.slick.driver.H2Driver.simple._
import java.util.Date

object DBTaskHistory {
  val javaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))
}

class DBTaskHistory(tag: Tag) extends Table[(Int, UUID, Date, SchedulerState)](tag, "DBTASK_HISTORY") {
  implicit val javaUtilDateMapper = DBTaskHistory.javaUtilDateMapper
  implicit val schedulerStateMapper = SchedulerState.mapper

  def history_id = column[Int]("HISTORY_ID", O.PrimaryKey, O.AutoInc)
  def task_id = column[UUID]("TASK_ID", O.NotNull)
  def state_change_time = column[Date]("STATE_CHANGE_TIME", O.NotNull)
  def scheduler_state = column[SchedulerState]("SCHEDULER_STATE", O.NotNull)
  override def * = (history_id, task_id, state_change_time, scheduler_state)
}