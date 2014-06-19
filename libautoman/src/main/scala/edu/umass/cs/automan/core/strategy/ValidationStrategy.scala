package edu.umass.cs.automan.core.strategy

import scala.collection.mutable
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
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

abstract class ValidationStrategy[Q <: Question, A <: Answer, B](question: Q) {
  class PrematureValidationCompletionException(methodname: String, classname: String)
    extends Exception(methodname + " called prematurely in " + classname)

  val _computation_id = UUID.randomUUID()
  var _budget_committed: BigDecimal = 0.00
  var _num_possibilities: BigInt = 2
  var _thunks = List[Thunk[A]]()
//  var _unique_workers = 0

  def is_done: Boolean
  def num_possibilities: BigInt = _num_possibilities
  def num_possibilities_=(n: BigInt) { _num_possibilities = n }
  def select_answer : B
  def spawn(suffered_timeout: Boolean): List[Thunk[A]]
  def thunks_to_accept: List[Thunk[A]]
  def thunks_to_reject: List[Thunk[A]]

  protected def unique_by_date(ts: List[Thunk[_ <: Answer]]) = {
    // worker_id should always be set for RETRIEVED and PROCESSED
    val tw_groups = ts.groupBy( t => t.worker_id.get )
    // sort by creation date and take the first
    tw_groups.map{ case(worker_id,ts) =>
      ts.sortBy{ t => t.created_at }.head
    }.toList
  }

  protected def valid_thunks = {
    // thunks should be
    val ts =_thunks.filter(t =>
      t.state == SchedulerState.RETRIEVED ||  // retrieved from MTurk
        t.state == SchedulerState.PROCESSED     // OR recalled from a memo DB
    )

    // if a worker completed more than one, take the first
    unique_by_date(ts)
  }
}