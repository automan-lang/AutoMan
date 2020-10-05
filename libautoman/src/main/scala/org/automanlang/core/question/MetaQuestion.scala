package org.automanlang.core.question

import org.automanlang.core.AutomanAdapter
import org.automanlang.core.answer.AbstractAnswer
import org.automanlang.core.policy.aggregation.MetaAggregationPolicy
import org.automanlang.core.scheduler.MetaScheduler
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
