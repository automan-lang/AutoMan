package edu.umass.cs.automan.adapters.mturk.logging.tables

import scala.slick.driver.H2Driver.simple._

class DBWorker(tag: Tag) extends Table[(String, String, String)](tag, "DBWORKER") {
  def workerId = column[String]("WORKER_ID")
  def groupId = column[String]("GROUP_ID")
  def HITTypeId = column[String]("HIT_TYPE_ID")
  override def * = (workerId, groupId, HITTypeId)
}
