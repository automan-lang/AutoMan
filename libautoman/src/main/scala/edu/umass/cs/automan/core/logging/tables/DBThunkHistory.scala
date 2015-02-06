package edu.umass.cs.automan.core.logging.tables

import java.util.UUID
import edu.umass.cs.automan.core.scheduler.SchedulerState
import edu.umass.cs.automan.core.scheduler.SchedulerState.SchedulerState
import scala.slick.driver.DerbyDriver.simple._
import java.util.Date

class DBThunkHistory(tag: Tag) extends Table[(Int, UUID, Date, SchedulerState)](tag, "THUNK_HISTORY") {
  implicit val JavaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))

  implicit val schedulerStateMapper = SchedulerState.mapper

  def history_id = column[Int]("HISTORY_ID", O.DBType("UUID"), O.PrimaryKey, O.AutoInc)
  def thunk_id = column[UUID]("THUNK_ID", O.DBType("UUID"))
  def state_change_time = column[Date]("STATE_CHANGE_TIME")
  def scheduler_state = column[SchedulerState]("SCHEDULER_STATE")
  override def * = (history_id, thunk_id, state_change_time, scheduler_state)
}
