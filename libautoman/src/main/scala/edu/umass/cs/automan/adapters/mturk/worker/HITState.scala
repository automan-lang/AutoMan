package edu.umass.cs.automan.adapters.mturk.worker

import java.util.UUID
import com.amazonaws.mturk.requester.{Assignment, HIT}
import edu.umass.cs.automan.adapters.mturk.mock.MockRequesterService
import edu.umass.cs.automan.core.scheduler.Task

object HITState {
  // creates a HITState object from a HIT data
  // structure and a list of tasks
  def apply(hit: HIT, ts: List[Task], hittype: HITType) : HITState = {
    val t_a_map = ts.map(_.task_id -> None).toMap
    HITState(hit, t_a_map, hittype, cancelled = false)
  }

  def distinctBy[T,U](xs: Seq[T])(pred: T => U): Seq[T] = {
    xs.groupBy(pred(_)).flatMap { case (key,xs_filt) => xs_filt.headOption }.toSeq
  }
}

// Note that this class stores references to actual MTurk Assignment objects
// but compares them by their Assignment IDs, in case Assignments
// are fetched multiple times.
case class HITState(hit: HIT, t_a_map: Map[UUID,Option[Assignment]], hittype: HITType, cancelled: Boolean) {
  val aid_t_map = t_a_map.flatMap { case (t, a_o) => a_o match { case Some(a) => Some(a.getAssignmentId -> t); case None => None }}

  def matchAssignments(assns: Array[Assignment], mock_service: Option[MockRequesterService]) : HITState = {
    // for every available assignment and unmatched task,
    // pair the two and update the map
    // we compare IDs since we may get duplicate Assignment
    // and task objects as we run
    val assns_distinct = HITState.distinctBy(assns){ a => a.getAssignmentId }
    assert(assns.length == assns_distinct.length)

    val (new_t_a_map,_) = assns.foldLeft(t_a_map,unmatchedTasks){ case ((tam, ts), a) =>
        if (!aid_t_map.contains(a.getAssignmentId) && ts.nonEmpty) {
          (tam + (ts.head -> Some(a)), ts.tail)
        } else {
          (tam, ts)
        }
    }

    // return new state object
    HITState(hit, new_t_a_map, hittype, cancelled)
  }

  def isCancelled : Boolean = cancelled
  def cancel() : HITState = HITState(hit, t_a_map, hittype, cancelled = true)

  private def unmatchedTasks : List[UUID] = {
    t_a_map.flatMap { case (t_id: UUID, a_o: Option[Assignment]) =>
      if (a_o.isEmpty) Some(t_id) else None
    }.toList
  }

  def getAssignmentOption(t: Task): Option[Assignment] = t_a_map(t.task_id)

  def addNewTasks(updated_hit: HIT, ts: List[Task]) : HITState = {
    assert(!cancelled)
    assert(updated_hit.getHITId == hit.getHITId)
    HITState(updated_hit, t_a_map ++ ts.map(_.task_id -> None), hittype, cancelled)
  }

  def HITId = hit.getHITId
  def HITType = hittype
}
