package edu.umass.cs.automan.core.logging.tables

import java.util.UUID
import edu.umass.cs.automan.core.info.QuestionType._

import scala.slick.driver.SQLiteDriver.simple._

object DBQuestion {
  val questionTypeMapper =
    MappedColumnType.base[QuestionType, Int](
    {
      case CheckboxQuestion => 0
      case CheckboxDistributionQuestion => 1
      case FreeTextQuestion => 2
      case FreeTextDistributionQuestion => 3
      case RadioButtonQuestion => 4
      case RadioButtonDistributionQuestion => 5
    },
    {
      case 0 => CheckboxQuestion
      case 1 => CheckboxDistributionQuestion
      case 2 => FreeTextQuestion
      case 3 => FreeTextDistributionQuestion
      case 4 => RadioButtonQuestion
      case 5 => RadioButtonDistributionQuestion
    }
    )
}

class DBQuestion(tag: Tag) extends Table[(UUID, String, QuestionType, String, String)](tag, "DBQUESTION") {
  implicit val questionTypeMapper = DBQuestion.questionTypeMapper

  def id = column[UUID]("QUESTION_ID", O.PrimaryKey)
  def memo_hash = column[String]("MEMO_HASH")
  def question_type = column[QuestionType]("QUESTION_TYPE")
  def text = column[String]("TEXT")
  def title = column[String]("TITLE")

  override def * = (id, memo_hash, question_type, text, title)
}