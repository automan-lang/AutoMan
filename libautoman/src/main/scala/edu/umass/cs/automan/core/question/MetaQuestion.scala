package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.MetaScheduler

import scala.concurrent._

trait MetaQuestion {
  type A <: Answer

  def schedulerFuture() : Future[A] = {
    Future{
      blocking {
        new MetaScheduler(this).run()
      }
    }
  }
}
