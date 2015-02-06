package edu.umass.cs.automan.core.logging.tables

import java.util.UUID
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType._

import scala.slick.driver.DerbyDriver.simple._

class DBQuestion(tag: Tag) extends Table[(UUID, String, QuestionType)](tag, "QUESTION") {
  implicit val questionTypeMapper = QuestionType.mapper

  def id = column[UUID]("QUESTION_ID", O.PrimaryKey, O.DBType("UUID"))
  def memo_hash = column[String]("MEMO_HASH")
  def question_type = column[QuestionType]("QUESTION_TYPE")

  override def * = (id, memo_hash, question_type)
}