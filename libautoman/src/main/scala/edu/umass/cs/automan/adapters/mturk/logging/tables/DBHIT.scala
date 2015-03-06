package edu.umass.cs.automan.adapters.mturk.logging.tables

import scala.slick.driver.DerbyDriver.simple._

class DBHIT(tag: Tag) extends Table[(String)](tag, "DBHIT") {
  def HITId = column[String]("HIT_ID", O.PrimaryKey)
  override def * = (HITId)
}
