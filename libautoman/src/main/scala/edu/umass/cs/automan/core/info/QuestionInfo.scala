package edu.umass.cs.automan.core.info

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.scheduler.Thunk
import scala.slick.driver.DerbyDriver.simple.MappedColumnType

case class QuestionInfo(computation_id: UUID,
                        name: String,
                        question_text: String,
                        question_desc: String,
                        question_type: QuestionType,
                        start_time: Date,
                        confidence_level: Double,
                        thunks: List[Thunk],
                        total_answers_needed: Int,
                        total_budget: BigDecimal,
                        budget_used: BigDecimal,
                        dont_reject: Boolean,
                        epochs: List[EpochInfo])

object QuestionType extends Enumeration {
  type QuestionType = Value
  val CheckboxQuestion = Value("CheckboxQuestion")
  val CheckboxDistributionQuestion = Value("CheckboxDistributionQuestion")
  val FreeTextQuestion = Value("FreeTextQuestion")
  val FreeTextDistributionQuestion = Value("FreeTextDistributionQuestion")
  val RadioButtonQuestion = Value("RadioButtonQuestion")
  val RadioButtonDistributionQuestion = Value("RadioButtonDistributionQuestion")

  // datatypes for serialization
//  val mapper =
//    MappedColumnType.base[QuestionType, Int](
//      {
//        case CheckboxQuestion => 0
//        case CheckboxDistributionQuestion => 1
//        case FreeTextQuestion => 2
//        case FreeTextDistributionQuestion => 3
//        case RadioButtonQuestion => 4
//        case RadioButtonDistributionQuestion => 5
//      },
//      {
//        case 0 => CheckboxQuestion
//        case 1 => CheckboxDistributionQuestion
//        case 2 => FreeTextQuestion
//        case 3 => FreeTextDistributionQuestion
//        case 4 => RadioButtonQuestion
//        case 5 => RadioButtonDistributionQuestion
//      }
//    )
}