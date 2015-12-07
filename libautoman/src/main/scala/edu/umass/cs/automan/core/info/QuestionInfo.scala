package edu.umass.cs.automan.core.info

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.scheduler.Task
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
  val CheckboxQuestion = Value("CheckboxQuestion")
  val CheckboxDistributionQuestion = Value("CheckboxDistributionQuestion")
  val EstimationQuestion = Value("EstimationQuestion")
  val FreeTextQuestion = Value("FreeTextQuestion")
  val FreeTextDistributionQuestion = Value("FreeTextDistributionQuestion")
  val RadioButtonQuestion = Value("RadioButtonQuestion")
  val RadioButtonDistributionQuestion = Value("RadioButtonDistributionQuestion")
}