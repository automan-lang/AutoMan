package org.automanlang.adapters.mturk.logging.tables

import scala.slick.driver.H2Driver.simple._

class DBHITType(tag: Tag) extends Table[(String, String, BigDecimal, Int, Int)](tag, "DBHITType") {
  def id = column[String]("HITTypeId", O.PrimaryKey)
  def groupId = column[String]("groupId")
  def cost = column[BigDecimal]("cost", O.DBType("decimal(10, 4)"))
  def timeoutInS = column[Int]("timeout")
  def batchNo = column[Int]("batchNo")
  override def * = (id, groupId, cost, timeoutInS, batchNo)
}
