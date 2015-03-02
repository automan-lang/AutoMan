package edu.umass.cs.automan.adapters.mturk.connectionpool

import java.util.UUID

import com.amazonaws.mturk.requester.{Assignment, HIT}
import edu.umass.cs.automan.core.scheduler.Thunk

object HITState {
  // creates a HITState object from a HIT data
  // structure and a list of Thunks
  def apply(hit: HIT, ts: List[Thunk[_]]) : HITState = {
    val t_a_map = ts.map(_.thunk_id -> None).toMap
    HITState(hit, t_a_map)
  }
}

case class HITState(hit: HIT, t_a_map: Map[UUID,Option[Assignment]]) {
  val aid_t_map = t_a_map.flatMap { case (t, a_o) => a_o match { case Some(a) => Some(a.getAssignmentId -> t); case None => None }}

  def matchAssignments(assns: List[Assignment]) : HITState = {
    val unmatched_thunks = t_a_map.flatMap { case (t_id: UUID, a_o: Option[Assignment]) => if (a_o == None) Some(t_id) else None }.toList

    // for every available assignment and unmatched thunk,
    // pair the two and update the map
    // we compare IDs since we may get duplicate Assignment
    // and Thunk objects as we run
    val (new_t_a_map,_) = assns.foldLeft(t_a_map,unmatched_thunks){ case ((tam, ts), a) =>
        if (!aid_t_map.contains(a.getAssignmentId) && ts.nonEmpty) {
          (tam + (ts.head -> Some(a)), ts.tail)
        } else {
          (tam, ts)
        }
    }

    // return new state object
    HITState(hit, new_t_a_map)
  }

  def addNewThunks(updated_hit: HIT, ts: List[Thunk[_]]) : HITState = {
    assert(updated_hit.getHITId == hit.getHITId)
    HITState(updated_hit, t_a_map ++ ts.map(_.thunk_id -> None))
  }

  def HITId = hit.getHITId
}
