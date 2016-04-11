package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.AbstractAnswer
import edu.umass.cs.automan.core.policy.aggregation.MetaAggregationPolicy
import edu.umass.cs.automan.core.scheduler.MetaScheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

trait MetaQuestion {
  type MA <: Any
  type MAA <: AbstractAnswer[MA]
  type MAP <: MetaAggregationPolicy

  def metaSchedulerFuture() : Future[MAA] = {
    Future{
      blocking {
        new MetaScheduler(this).run().asInstanceOf[MAA]
      }
    }
  }

  def computeAnswer(round: Int) : MAA
  def done: Boolean
}
