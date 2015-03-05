package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class RadioButtonQuestion extends ScalarQuestion[Symbol] {
  type QuestionOptionType <: QuestionOption

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QuestionOptionType]

  override protected[automan] def getOutcome(scheduler: Scheduler[Symbol]): ScalarOutcome[Symbol] = {
    ScalarOutcome( Future{ blocking { scheduler.run() } } )
  }

  override protected[automan] def getQuestionType = QuestionType.RadioButtonQuestion
}
