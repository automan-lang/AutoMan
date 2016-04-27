package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.AbstractAnswer
import edu.umass.cs.automan.core.policy.aggregation.MetaAggregationPolicy
import edu.umass.cs.automan.core.scheduler.MetaScheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

trait MetaQuestion {
  type MA <: Any
  type MAA <: AbstractAnswer[MA]
  type MAP <: MetaAggregationPolicy

  def metaSchedulerFuture(backend: AutomanAdapter) : Future[MAA] = {
    Future{
      blocking {
        new MetaScheduler(this, backend).run().asInstanceOf[MAA]
      }
    }
  }

  def metaAnswer(round: Int, backend: AutomanAdapter) : MAA
  def done(round: Int, backend: AutomanAdapter) : Boolean
}
