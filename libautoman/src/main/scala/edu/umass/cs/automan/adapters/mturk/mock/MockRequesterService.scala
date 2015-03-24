package edu.umass.cs.automan.adapters.mturk.mock

import java.lang
import java.lang.Double
import com.amazonaws.mturk.addon.BatchItemCallback
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.ClientConfig
import edu.umass.cs.automan.core.util._
import java.util.{GregorianCalendar, Calendar, UUID, Date}

object HITBackendStatus extends Enumeration {
  type HITBackendStatus = Value
  val RUNNING,
      EXPIRED
  = Value
}
object AssignmentStatus extends Enumeration {
  type AssignmentStatus = Value
  val ANSWERED,
      UNANSWERED,
      ACCEPTED,
      REJECTED
  = Value
}
import HITBackendStatus._
import AssignmentStatus._

case class MockServiceState(budget: Double,
                            assignments_by_question_id: Map[String, (Assignment, AssignmentStatus.Value)],
                            hits: List[HIT],
                            hit_type_ids: List[String],
                            hitstates: Map[String, HITBackendStatus.Value]) {
  def this(budget: Double, assignments: List[Assignment]) =
    this(budget, assignments.map{ a => a.getAssignmentId -> (a, AssignmentStatus.UNANSWERED) }.toMap, List.empty, List.empty, Map.empty)
  def updateAssignments(updated_assignments: Map[String, (Assignment, AssignmentStatus.Value)]): MockServiceState = {
    MockServiceState(budget, updated_assignments, hits, hit_type_ids, hitstates)
  }
  def updateHITState(hit_id: String, state: HITBackendStatus.Value): MockServiceState = {
    MockServiceState(budget, assignments_by_question_id, hits, hit_type_ids, hitstates + (hit_id -> state))
  }
  def addHIT(h: HIT): MockServiceState = {
    MockServiceState(budget, assignments_by_question_id, h :: hits, hit_type_ids, hitstates)
  }
  def extendHIT(hitId: String, deltaSec: Int, deltaAssignments: Int) = {
    assert(hits.size >= 1)
    val (hit,not_matches) = hits.partition { h => h.getHITId == hitId } match { case (hits,nms) => (hits.head, nms) }

    val cloned_hit = hit.clone().asInstanceOf[HIT]
    cloned_hit.setExpiration(Utilities.calInSeconds(cloned_hit.getExpiration, deltaSec))
    cloned_hit.setMaxAssignments(cloned_hit.getMaxAssignments + deltaAssignments)

    MockServiceState(budget, assignments_by_question_id, cloned_hit :: not_matches, hit_type_ids, hitstates)
  }
  def getAssignmentsForHITId(hit_id: String) : List[Assignment] = {
    assignments_by_question_id.filter { case (_,(assn,_)) => assn.getHITId == hit_id }.map { case (_,(assn,_)) => assn }.toList
  }
  def getAssignmentsByAssignmentId : Map[String,(Assignment,AssignmentStatus.Value,String)] = {
    assignments_by_question_id.map { case (question_id, (assn, assn_status)) => assn.getAssignmentId -> (assn, assn_status, question_id)}
  }
}

class MockRequesterService(initial_state: MockServiceState, config: ClientConfig) extends RequesterService(config) {
  var _state = initial_state

  override def approveAssignments(assignmentIds: Array[String],
                                  requesterFeedback: Array[String],
                                  defaultFeedback: String,
                                  callback: BatchItemCallback): Unit = {
    val dedup_ids = assignmentIds.distinct

    assert(dedup_ids.length == assignmentIds.length)
    assert(assignmentIds.length > 0)

    val assns2 = assignmentIds.foldLeft(_state.assignments_by_question_id){ case (a_map, id) =>

      // get assignments by assignment_id
      val assns_by_a_id = _state.getAssignmentsByAssignmentId

      // the assignment is in the map and has not already been answered
      assert (assns_by_a_id.contains(id) && assns_by_a_id(id)._2 == AssignmentStatus.UNANSWERED)

      // update map: question_id -> (assignment, assignment_status)
      a_map + (assns_by_a_id(id)._3 -> (assns_by_a_id(id)._1, AssignmentStatus.ANSWERED))
    }
    _state = _state.updateAssignments(assns2)
  }

  override def forceExpireHIT(hitId: String): Unit = {
    _state = _state.updateHITState(hitId, HITBackendStatus.EXPIRED)
  }

  override def createHIT(hitTypeId: String,
                         title: String,
                         description: String,
                         keywords: String,
                         question: String,
                         reward: Double,
                         assignmentDurationInSeconds: lang.Long,
                         autoApprovalDelayInSeconds: lang.Long,
                         lifetimeInSeconds: lang.Long,
                         maxAssignments: Integer,
                         requesterAnnotation: String,
                         qualificationRequirements: Array[QualificationRequirement],
                         responseGroup: Array[String]): HIT = {
    assert(_state.hit_type_ids.contains(hitTypeId))
    val now = Utilities.nowCal()
    val expiry = Utilities.calInSeconds(now, lifetimeInSeconds.toInt)

    val hit = new HIT(
        null,                                       // request
        UUID.randomUUID().toString,                 // HIT ID
        hitTypeId,                                  // HIT Type ID
        null,                                       // HIT Group ID
        null,                                       // HIT Layout ID
        now,                                       // creationTime
        title,                                      // title
        description,                                // description
        question,                                   // question
        keywords,                                   // keywords
        HITStatus.Assignable,                       // HIT Status
        maxAssignments,                             // maxAssignments
        new Price(new java.math.BigDecimal(reward), "USD", "$"),  // reward
        autoApprovalDelayInSeconds,                 // autoApprovalDelayInSeconds
        expiry,                                     // expiration
        assignmentDurationInSeconds,                // assignmentDurationInSeconds
        requesterAnnotation,                        // requesterAnnotation
        qualificationRequirements,                  // qualificationRequirements
        HITReviewStatus.NotReviewed,                // HITReviewStatus
        0,                                          // numberOfAssignmentsPending
        maxAssignments,                             // numberOfAssignmentsAvailable
        0                                           // numberOfAssignmentsCompleted
      )
    _state = _state.addHIT(hit)
    hit
  }

  override def getHIT(hitId: String): HIT = {
    assert(_state.hits.size >= 1)
    val matching_hits = _state.hits.filter { h => h.getHITId == hitId }
    assert(matching_hits.size == 1)
    matching_hits.head
  }

  override def extendHIT(hitId: String,
                         maxAssignmentsIncrement: Integer,
                         expirationIncrementInSeconds: lang.Long): Unit = {
    _state = _state.extendHIT(hitId, expirationIncrementInSeconds.toInt, maxAssignmentsIncrement)
  }

  override def rejectAssignment(assignmentId: String, requesterFeedback: String): Unit = {
    val (assn,assn_state) = _state.getAssignmentsByAssignmentId(assignmentId)

    assert(assn_state == AssignmentStatus.ANSWERED)

    _state = _state.updateAssignments(_state.assignments_by_question_id + (assignmentId -> (assn, AssignmentStatus.REJECTED)))
  }

  override def getAllAssignmentsForHIT(hitId: String): Array[Assignment] = {
    ???
  }

  override def assignQualification(qualificationTypeId: String,
                                   workerId: String,
                                   integerValue: Integer,
                                   sendNotification: Boolean): Unit = {
    ???
  }

  override def rejectQualificationRequest(qualificationRequestId: String,
                                          reason: String): Unit = {
    ???
  }

  override def getAccountBalance: Double = {
    ???
  }

  override def revokeQualification(qualificationTypeId: String,
                                   subjectId: String,
                                   reason: String): Unit = {
    ???
  }

  override def disposeQualificationType(qualificationTypeId: String): QualificationType = {
    ???
  }

  override def getAllQualificationRequests(qualificationTypeId: String): Array[QualificationRequest] = {
    ???
  }

  override def grantQualification(qualificationRequestId: String,
                                  integerValue: Integer): Unit = {
    ???
  }

  override def createQualificationType(name: String,
                                       keywords: String,
                                       description: String): QualificationType = {
    ???
  }
}
