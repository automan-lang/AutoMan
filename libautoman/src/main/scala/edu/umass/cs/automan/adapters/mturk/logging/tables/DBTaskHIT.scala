package edu.umass.cs.automan.adapters.MTurk.logging.tables

import java.util.UUID
import scala.slick.driver.SQLiteDriver.simple._

class DBTaskHIT(tag: Tag) extends Table[(String, UUID)](tag, "DBTASKHIT") {
  def HITId = column[String]("HIT_ID", O.PrimaryKey)
  def taskId = column[UUID]("TASK_ID")
  override def * = (HITId, taskId)
}
