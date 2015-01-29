package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Scheduler

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class RadioButtonQuestion extends ScalarQuestion[Symbol] {
  type QO <: QuestionOption

  protected var _options: List[QO]

  def options: List[QO]
  def options_=(os: List[QO])
  def num_possibilities: BigInt = BigInt(_options.size)
  def randomized_options: List[QO]

  override protected[automan] def getAnswer(scheduler: Scheduler[Symbol]): Answer[Symbol] = {
    val f = Future{ blocking { scheduler.run() } }
    new Answer[Symbol](f, scheduler)
  }
}