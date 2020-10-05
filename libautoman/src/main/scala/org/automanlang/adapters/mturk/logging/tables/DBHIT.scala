package org.automanlang.adapters.mturk.logging.tables

import scala.slick.driver.H2Driver.simple._

class DBHIT(tag: Tag) extends Table[(String, String, Boolean)](tag, "DBHIT") {
  def HITId = column[String]("HIT_ID", O.PrimaryKey)
  def HITTypeId = column[String]("HIT_TYPE_ID")
  def isCancelled = column[Boolean]("IS_CANCELLED")
  override def * = (HITId, HITTypeId, isCancelled)
}
