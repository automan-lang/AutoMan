package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class RadioButtonQuestion extends ScalarQuestion {
  type A = Symbol
  type QuestionOptionType <: QuestionOption

  protected var _options: List[QuestionOptionType] = List[QuestionOptionType]()

  def options: List[QuestionOptionType] = _options
  def options_=(os: List[QuestionOptionType]) { _options = os }
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QuestionOptionType]

  override protected[automan] def getQuestionType = QuestionType.RadioButtonQuestion

  override protected[automan] def getOutcome(adapter: AutomanAdapter, memo: Memo, poll_interval_in_s: Int) : O = {
    val scheduler = new Scheduler(this, adapter, memo, poll_interval_in_s)
    val f = Future{
      blocking {
        scheduler.run().asInstanceOf[AA]
      }
    }
    ScalarOutcome(f)
  }
}
