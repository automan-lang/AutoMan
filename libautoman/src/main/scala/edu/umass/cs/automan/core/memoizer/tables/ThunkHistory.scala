package edu.umass.cs.automan.core.memoizer.tables

import java.util.UUID
import edu.umass.cs.automan.core.scheduler.SchedulerState
import edu.umass.cs.automan.core.scheduler.SchedulerState.SchedulerState
import scala.slick.driver.DerbyDriver.simple._
import java.util.Date

class ThunkHistory(tag: Tag) extends Table[(UUID, Date, SchedulerState, Option[String])](tag, "THUNK_HISTORY") {
  implicit val JavaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))

  implicit val schedulerStateMapper = SchedulerState.mapper

  def thunk_id = column[UUID]("THUNK_ID", O.DBType("UUID"))
  def state_change_time = column[Date]("STATE_CHANGE_TIME")
  def scheduler_state = column[SchedulerState]("SCHEDULER_STATE")
  def worker_id = column[Option[String]]("WORKER_ID")
  override def * = (thunk_id, state_change_time, scheduler_state, worker_id)
}
