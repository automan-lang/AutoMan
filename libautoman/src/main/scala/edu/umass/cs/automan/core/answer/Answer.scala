package edu.umass.cs.automan.core.answer

import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.scheduler.{SchedulerResult, Scheduler}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Answer[T](private val f: Future[SchedulerResult[T]], private val scheduler: Scheduler[T]) {
  def cost : AbstractOutcome[BigDecimal] = {
    try {
      Outcome(Await.result(f, Duration.Inf).cost)
    } catch {
      case e:OverBudgetException[T] => OverBudget()
    }
  }
  def value : AbstractOutcome[T] = {
    try {
      val result = Await.result(f, Duration.Inf).answer
      Outcome(result)
    } catch {
      case e:OverBudgetException[T] => OverBudget()
    }
  }
}
