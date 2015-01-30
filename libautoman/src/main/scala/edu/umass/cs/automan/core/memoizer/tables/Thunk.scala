package edu.umass.cs.automan.core.memoizer.tables

import java.util.UUID

import edu.umass.cs.automan.core.scheduler.SchedulerState.SchedulerState

import scala.slick.driver.DerbyDriver.simple._
import java.util.Date

class Thunk(tag: Tag) extends Table[(UUID, Date, UUID, BigDecimal, Date, UUID, SchedulerState, Int, String, Int)](tag, "THUNK") {
  implicit val JavaUtilDateMapper =
    MappedColumnType .base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))

  def id = column[UUID]("THUNK_ID", O.PrimaryKey, O.DBType("UUID"))
  def completion_time = column[Date]("COMPLETION_TIME")
  def computation_id = column[UUID]("COMPUTATION_ID", O.DBType("UUID"))
  def cost_in_cents = column[BigDecimal]("COST_IN_CENTS", O.DBType("decimal(10, 4)"))
  def creation_time = column[Date]("CREATION_TIME")
  def question_id = column[UUID]("QUESTION_ID", O.DBType("UUID"))
  def scheduler_state = column[SchedulerState]("SCHEDULER_STATE")
  def timeout_in_s = column[Int]("TIMEOUT_IN_SEC")
  def worker_id = column[String]("WORKER_ID")
  def worker_timeout_in_s = column[Int]("WORKER_TIMEOUT_IN_SEC")
  override def * = (id, completion_time, computation_id, cost_in_cents, creation_time, question_id, scheduler_state, timeout_in_s, worker_id, worker_timeout_in_s)
}