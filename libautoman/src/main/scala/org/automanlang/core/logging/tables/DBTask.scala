package org.automanlang.core.logging.tables

import java.util.UUID
import scala.slick.driver.H2Driver.simple._
import java.util.Date

object DBTask {
  val javaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))
}

class DBTask(tag: Tag) extends Table[(UUID, UUID, Int, BigDecimal, Date, Int, Int)](tag, "DBTASK") {
  implicit val javaUtilDateMapper = DBTask.javaUtilDateMapper

  def task_id = column[UUID]("TASK_ID", O.PrimaryKey)
  def question_id = column[UUID]("QUESTION_ID")
  def round = column[Int]("ROUND")
  def cost = column[BigDecimal]("COST_IN_CENTS", O.DBType("decimal(10, 4)"))
  def creation_time = column[Date]("CREATION_TIME")
  def timeout_in_s = column[Int]("TIMEOUT_IN_S")
  def worker_timeout_in_s = column[Int]("WORKER_TIMEOUT_IN_S")
  override def * = (task_id, question_id, round, cost, creation_time, timeout_in_s, worker_timeout_in_s)
  def ? = (task_id.?, question_id.?, round.?, cost.?, creation_time.?, timeout_in_s.?, worker_timeout_in_s.?)
}