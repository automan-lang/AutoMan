package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.exception.OverBudgetException
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

import scala.collection.mutable
import edu.umass.cs.automan.core.scheduler.{SchedulerResult, SchedulerState, Thunk}
import edu.umass.cs.automan.core.question.Question
import java.util.UUID

object ValidationStrategy {
  // this data structure tracks worker re-participation
  // across all assignments for like-tasks
  // key: (title, text, computation_id)
  // value: (num_workers, num_tasks)
  // intra-question figures are inserted by the ValidationStrategy
  // inter-question figures are computed by summing the values for a
  // fixed (title, description) and a free UUID.
  private val _global_uniqueness = new mutable.HashMap[(String,String,UUID),(Int,Int)]()
  protected[strategy] def overwrite(title: String, text: String, computation_id: UUID, num_workers: Int, num_tasks: Int) {
    val key: (String, String, UUID) = (title, text, computation_id)
    val value: (Int, Int) = (num_workers, num_tasks)

    this.synchronized {
      if (_global_uniqueness.contains((title,text,computation_id))) {
        // update
        _global_uniqueness.update(key,value)
      } else {
        // initial insert
        _global_uniqueness.put(key, value)
      }
    }
  }

  // the proportion of work that is done by unique workers
  protected[strategy] def work_uniqueness(title: String, text: String) : Option[Double] = {
    this.synchronized {
      val stats: List[(Int, Int)] =
        _global_uniqueness.filter{ case (key, _) =>
          // get matching elements
          val (gtitle,gtext,_) = key
          title.equals(gtitle) && text.equals(gtext)
          // extract values as list of tuples
        }.toList.map { case (key, value) =>
          val (num_workers,num_tasks) = value
          (num_workers,num_tasks)
        }
      val total_workers: Int = stats.map { _._1 }.sum
      val total_tasks: Int = stats.map { _._2 }.sum
      if (total_workers == 0 || total_tasks == 0) {
        None
      } else {
        Some(total_workers.toDouble / total_tasks.toDouble)
      }
    }
  }

}

abstract class ValidationStrategy[A](question: Question[A]) {
  class PrematureValidationCompletionException(methodname: String, classname: String)
    extends Exception(methodname + " called prematurely in " + classname)

  val _computation_id = UUID.randomUUID()
  var _budget_committed: BigDecimal = 0.00
  var _num_possibilities: BigInt = 2

  def is_done(thunks: List[Thunk[A]]) : Boolean
  def num_possibilities: BigInt = _num_possibilities
  def num_possibilities_=(n: BigInt) { _num_possibilities = n }
  def select_answer(thunks: List[Thunk[A]]) : Option[SchedulerResult[A]]
  def spawn(thunks: List[Thunk[A]], suffered_timeout: Boolean): List[Thunk[A]]
  def thunks_to_accept(thunks: List[Thunk[A]]): List[Thunk[A]]
  def thunks_to_reject(thunks: List[Thunk[A]]): List[Thunk[A]]
  def pay_for_thunks(ts: List[Thunk[A]]) {
    ts.foreach { t =>
      _budget_committed += question.reward
      if (_budget_committed > question.budget) {
        Utilities.DebugLog("Over budget. budget_committed = " + _budget_committed + " > budget = " + question.budget, LogLevel.FATAL, LogType.STRATEGY, _computation_id)
        throw OverBudgetException[A](None)
      }
    }
  }
  def unpay_for_thunks(ts: List[Thunk[A]]) {
    ts.foreach { t =>
      _budget_committed -= question.reward
      Utilities.DebugLog("Returning " + question.reward + " to budget.", LogLevel.INFO, LogType.STRATEGY, _computation_id)
    }
  }

  protected def unique_by_date(ts: List[Thunk[A]]) = {
    // worker_id should always be set for RETRIEVED and PROCESSED
    val tw_groups = ts.groupBy( t => t.worker_id.get )
    // sort by creation date and take the first
    tw_groups.map{ case(worker_id,ts) =>
      ts.sortBy{ t => t.created_at }.head
    }.toList
  }

  protected def completed_thunks(thunks: List[Thunk[A]]) = {
    // thunks should be
    thunks.filter(t =>
      t.state == SchedulerState.RETRIEVED ||  // retrieved from MTurk
        t.state == SchedulerState.PROCESSED ||  // OR recalled from a memo DB
        t.state == SchedulerState.ACCEPTED      // OR accepted
    )
  }

  // thunks that have either been retrieved from memo
  // or pulled from backend; and no more than one per worker
  protected def completed_workerunique_thunks(thunks: List[Thunk[A]]) = {
    // if a worker completed more than one, take the first
    unique_by_date(completed_thunks(thunks))
  }

  protected def outstanding_thunks(thunks: List[Thunk[A]]) = {
    // basically, not TIMEOUTs and REJECTs
    val outstanding = thunks.filter(t =>
      t.state == SchedulerState.READY ||
      t.state == SchedulerState.RUNNING
    )
    // don't count duplicates
    val completed = completed_workerunique_thunks(thunks)
    outstanding ::: completed
  }
}