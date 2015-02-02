package edu.umass.cs.automan.core.memoizer.tables

import java.util.UUID
import scala.slick.driver.DerbyDriver.simple._

class QuestionThunk(tag: Tag) extends Table[(UUID, UUID)](tag, "QUESTIONTHUNK") {
  def question_id = column[UUID]("QUESTION_ID", O.PrimaryKey, O.DBType("UUID"))
  def thunk_id = column[UUID]("THUNK_ID", O.DBType("UUID"))
  override def * = (question_id, thunk_id)
}