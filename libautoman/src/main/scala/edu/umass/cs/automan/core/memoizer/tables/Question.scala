package edu.umass.cs.automan.core.memoizer.tables

import java.util.UUID
import scala.slick.driver.DerbyDriver.simple._

class Question(tag: Tag) extends Table[(UUID, String)](tag, "QUESTION") {
  def id = column[UUID]("QUESTION_ID", O.PrimaryKey, O.DBType("UUID"))
  def memo_hash = column[String]("MEMO_HASH")
  override def * = (id, memo_hash)
}