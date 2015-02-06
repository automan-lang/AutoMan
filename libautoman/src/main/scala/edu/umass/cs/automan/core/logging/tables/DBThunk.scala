package edu.umass.cs.automan.core.logging.tables

import java.util.UUID
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType

import scala.slick.driver.DerbyDriver.simple._
import java.util.Date

class DBThunk(tag: Tag) extends Table[(UUID, UUID, BigDecimal, Date, Int, Int)](tag, "THUNK") {
  implicit val javaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))

  def thunk_id = column[UUID]("THUNK_ID", O.PrimaryKey, O.DBType("UUID"))
  def question_id = column[UUID]("QUESTION_ID", O.DBType("UUID"))
  def cost_in_cents = column[BigDecimal]("COST_IN_CENTS", O.DBType("decimal(10, 4)"))
  def creation_time = column[Date]("CREATION_TIME")
  def timeout_in_s = column[Int]("TIMEOUT_IN_SEC")
  def worker_timeout_in_s = column[Int]("WORKER_TIMEOUT_IN_SEC")
  override def * = (thunk_id, question_id, cost_in_cents, creation_time, timeout_in_s, worker_timeout_in_s)
  def ? = (thunk_id.?, question_id.?, cost_in_cents.?, creation_time.?, timeout_in_s.?, worker_timeout_in_s.?)
})