package edu.umass.cs.automan.adapters.mturk.logging.tables

import scala.slick.driver.DerbyDriver.simple._

class DBQualificationRequirement(tag: Tag) extends Table[(String, Int, String)](tag, "DBQualificationRequirement") {
  def qualificationTypeId = column[String]("qualificationTypeId", O.PrimaryKey)
  def integerValue = column[Int]("integerValue")
  def HITTypeId = column[String]("HITTypeId")
  override def * = (qualificationTypeId, integerValue, HITTypeId)
}
