package edu.umass.cs.automan.adapters.mturk.logging.tables

import java.util.UUID
import scala.slick.driver.DerbyDriver.simple._

class DBAssignment(tag: Tag) extends Table[(String, UUID)](tag, "DBAssignment") {
  def AssignmentId = column[String]("AssignmentId", O.PrimaryKey)
  def ThunkId = column[UUID]("ThunkId")
  override def * = (AssignmentId, UUID)
}