package org.automanlang.adapters.mturk.logging.tables

import java.util.UUID
import scala.slick.driver.H2Driver.simple._

class DBTaskHIT(tag: Tag) extends Table[(String, UUID)](tag, "DBTASKHIT") {
  def HITId = column[String]("HIT_ID")
  def taskId = column[UUID]("TASK_ID")
  override def * = (HITId, taskId)
}
