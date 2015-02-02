package edu.umass.cs.automan.core.memoizer.tables

import java.util.UUID
import scala.slick.driver.DerbyDriver.simple._
import java.util.Date

class Thunk(tag: Tag) extends Table[(UUID, UUID, BigDecimal, Date, UUID, Int, Int)](tag, "THUNK") {
  implicit val javaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))

  def thunk_id = column[UUID]("THUNK_ID", O.PrimaryKey, O.DBType("UUID"))
  def computation_id = column[UUID]("COMPUTATION_ID", O.DBType("UUID"))
  def cost_in_cents = column[BigDecimal]("COST_IN_CENTS", O.DBType("decimal(10, 4)"))
  def creation_time = column[Date]("CREATION_TIME")
  def question_id = column[UUID]("QUESTION_ID", O.DBType("UUID"))
  def timeout_in_s = column[Int]("TIMEOUT_IN_SEC")
  def worker_timeout_in_s = column[Int]("WORKER_TIMEOUT_IN_SEC")
  override def * = (thunk_id, computation_id, cost_in_cents, creation_time, question_id, timeout_in_s, worker_timeout_in_s)
}
