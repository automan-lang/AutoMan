package edu.umass.cs.automan.adapters.MTurk.logging.tables

import scala.slick.driver.SQLiteDriver.simple._

class DBHITType(tag: Tag) extends Table[(String, String, BigDecimal, Int, Int)](tag, "DBHITType") {
  def id = column[String]("HITTypeId", O.PrimaryKey)
  def groupId = column[String]("groupId")
  def cost = column[BigDecimal]("cost", O.DBType("decimal(10, 4)"))
  def timeoutInS = column[Int]("timeout")
  def maxBatchNo = column[Int]("maxBatchNo")
  override def * = (id, groupId, cost, timeoutInS, maxBatchNo)
}
