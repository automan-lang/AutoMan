package edu.umass.cs.automan.adapters.mturk.logging.tables

import java.util.UUID
import scala.slick.driver.DerbyDriver.simple._

class DBThunkHIT(tag: Tag) extends Table[(String, UUID)](tag, "DBTHUNKHIT") {
  def HITId = column[String]("HIT_ID", O.PrimaryKey)
  def thunkId = column[UUID]("THUNK_ID")
  override def * = (HITId, thunkId)
}
