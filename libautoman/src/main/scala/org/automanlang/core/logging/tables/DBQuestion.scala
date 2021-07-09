package org.automanlang.core.logging.tables

import java.util.UUID
import org.automanlang.core.info.QuestionType._
import scala.slick.driver.H2Driver.simple._

object DBQuestion {
  val questionTypeMapper =
    MappedColumnType.base[QuestionType, Int](
    {
      case CheckboxQuestion => 0
      case CheckboxDistributionQuestion => 1
      case EstimationQuestion => 2
      case FreeTextQuestion => 3
      case FreeTextDistributionQuestion => 4
      case RadioButtonQuestion => 5
      case RadioButtonDistributionQuestion => 6
      case MultiEstimationQuestion => 7
      case Survey => 8
      case HugoQuestion => 9
      case FileDistributionQuestion => 10
      case SurveyQuestion => 11
    },
    {
      case 0 => CheckboxQuestion
      case 1 => CheckboxDistributionQuestion
      case 2 => EstimationQuestion
      case 3 => FreeTextQuestion
      case 4 => FreeTextDistributionQuestion
      case 5 => RadioButtonQuestion
      case 6 => RadioButtonDistributionQuestion
      case 7 => MultiEstimationQuestion
      case 8 => Survey
      case 9 => HugoQuestion
      case 10 => FileDistributionQuestion
      case 11 => SurveyQuestion
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