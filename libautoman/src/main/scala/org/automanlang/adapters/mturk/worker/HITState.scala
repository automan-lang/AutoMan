package org.automanlang.adapters.mturk.worker

import java.util.UUID

import com.amazonaws.services.mturk.model.{Assignment, HIT}
//import com.amazonaws.services.mturk.model.HIT

//import com.amazonaws.mturk.requester.{Assignment, HIT}
import software.amazon.awssdk.services.mturk._
import com.amazonaws.client.builder._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.mturk.{AmazonMTurk, AmazonMTurkClientBuilder}
//import software.amazon.awssdk.services.mturk.model.HIT
import org.automanlang.adapters.mturk.mock.MockRequesterService
import org.automanlang.core.scheduler.Task
import org.automanlang.core.util.Utilities

object HITState {
  // creates a HITState object from a HIT data
  // structure and a list of tasks
  def apply(hit: HIT, ts: List[Task], hittype: HITType) : HITState = {
    val t_a_map: Map[UUID, None.type] = ts.map(_.task_id -> None).toMap
    HITState(hit, t_a_map, hittype, cancelled = false)
  }
}

// Note that this class stores references to actual MTurk Assignment objects
// but compares them by their Assignment IDs, in case Assignments
// are fetched multiple times.
case class HITState(hit: HIT, t_a_map: Map[UUID,Option[Assignment]], hittype: HITType, cancelled: Boolean) {
  val aid_t_map: Map[String, UUID] = t_a_map.flatMap { case (t, a_o) => a_o match { case Some(a) => Some(a.getAssignmentId -> t); case None => None }}
  // pulls UUIDs and maps assignment associated with it to UUID

  def matchAssignments(assns: Array[Assignment], mock_service: Option[MockRequesterService]) : HITState = {
    // make sure that our list of assignments only
    // contains new, unique assignments
    val gathered_assn_ids = t_a_map.values.flatten.map(_.getAssignmentId).toList
    val assns_new = Utilities.distinctBy(assns){ a => a.getAssignmentId }
                             .filterNot { a => gathered_assn_ids.contains(a.getAssignmentId) }
                             .sortWith { (a1, a2) => a1.getSubmitTime.before(a2.getSubmitTime) }

    // for every available assignment and unmatched task,
    // pair the two and update the map
    // we compare IDs since we may get duplicate Assignment
    // and task objects as we run
    val (new_t_a_map,_) = assns_new.foldLeft(t_a_map,unmatchedTasks){ case ((tam, ts), a) =>
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

  def HITId : String = hit.getHITId()
  def HITType = hittype
}
