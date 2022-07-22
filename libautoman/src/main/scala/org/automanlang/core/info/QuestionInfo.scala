package org.automanlang.core.info

import java.util.{Date, UUID}

import org.automanlang.core.info.QuestionType.QuestionType
import org.automanlang.core.scheduler.Task
import scala.slick.driver.SQLiteDriver.simple.MappedColumnType

case class QuestionInfo(computation_id: UUID,
                        name: String,
                        question_text: String,
                        question_desc: String,
                        question_type: QuestionType,
                        start_time: Date,
                        confidence_level: Double,
                        tasks: List[Task],
                        total_answers_needed: Int,
                        total_budget: BigDecimal,
                        budget_used: BigDecimal,
                        dont_reject: Boolean,
                        epochs: List[EpochInfo])

object QuestionType extends Enumeration {
  type QuestionType = Value
  val CheckboxQuestion: QuestionType = Value("CheckboxQuestion")
  val CheckboxDistributionQuestion: QuestionType = Value("CheckboxDistributionQuestion")
  val MultiEstimationQuestion: QuestionType = Value("MultiEstimationQuestion")
  val EstimationQuestion: QuestionType = Value("EstimationQuestion")
  val FreeTextQuestion: QuestionType = Value("FreeTextQuestion")
  val FreeTextDistributionQuestion: QuestionType = Value("FreeTextDistributionQuestion")
  val RadioButtonQuestion: QuestionType = Value("RadioButtonQuestion")
  val RadioButtonDistributionQuestion: QuestionType = Value("RadioButtonDistributionQuestion")
  val Survey: QuestionType = Value("Survey")
  val VariantQuestion: QuestionType = Value("VariantQuestion")
}
