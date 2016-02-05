package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class CheckboxVectorQuestion extends VectorQuestion {
  type A = Set[Symbol]
  type AA = Answers[A]
  type O = VectorOutcome[A]
  type QuestionOptionType <: QuestionOption

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = {
    val base = BigInt(2)
    base.pow(options.size)
  }
  def randomized_options: List[QuestionOptionType]

  override protected[automan] def getQuestionType = QuestionType.CheckboxDistributionQuestion
}
