package edu.umass.cs.automan.adapters.mturk.mock

import java.lang
import java.lang.{Boolean, Double}
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

case class MockServiceState(budget: java.math.BigDecimal,
                            assignments_by_question_id: Map[String, List[(Assignment, AssignmentStatus.Value)]],
                            hits: List[HIT],
                            hit_type_ids: List[String],
                            hitstates: Map[String, HITBackendStatus.Value]) {
  def updateAssignments(updated_assignments: Map[String, List[(Assignment, AssignmentStatus.Value)]]): MockServiceState = {
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

    // clone
    val cloned_hit = cloneHIT(hit)

    // update selected fields
    cloned_hit.setExpiration(Utilities.calInSeconds(cloned_hit.getExpiration, deltaSec))
    cloned_hit.setMaxAssignments(cloned_hit.getMaxAssignments + deltaAssignments)

    MockServiceState(budget, assignments_by_question_id, cloned_hit :: not_matches, hit_type_ids, hitstates)
  }
  private def cloneHIT(hit: HIT) : HIT = {
    val cloned_hit = new HIT()
    cloned_hit.setAssignmentDurationInSeconds(hit.getAssignmentDurationInSeconds)
    cloned_hit.setAutoApprovalDelayInSeconds(hit.getAutoApprovalDelayInSeconds)
    cloned_hit.setCreationTime(hit.getCreationTime)
    cloned_hit.setDescription(hit.getDescription)
    cloned_hit.setExpiration(hit.getExpiration)
    cloned_hit.setHITGroupId(hit.getHITGroupId)
    cloned_hit.setHITId(hit.getHITId)
    cloned_hit.setHITLayoutId(hit.getHITLayoutId)
    cloned_hit.setHITReviewStatus(hit.getHITReviewStatus)
    cloned_hit.setHITStatus(hit.getHITStatus)
    cloned_hit.setHITTypeId(hit.getHITTypeId)
    cloned_hit.setKeywords(hit.getKeywords)
    cloned_hit.setMaxAssignments(hit.getMaxAssignments)
    cloned_hit.setNumberOfAssignmentsAvailable(hit.getNumberOfAssignmentsAvailable)
    cloned_hit.setNumberOfAssignmentsCompleted(hit.getNumberOfAssignmentsCompleted)
    cloned_hit.setNumberOfAssignmentsPending(hit.getNumberOfAssignmentsPending)
    cloned_hit.setQualificationRequirement(hit.getQualificationRequirement)
    cloned_hit.setQuestion(hit.getQuestion)
    cloned_hit.setRequest(hit.getRequest)
    cloned_hit.setRequesterAnnotation(hit.getRequesterAnnotation)
    cloned_hit.setReward(hit.getReward)
    cloned_hit.setTitle(hit.getTitle)
    cloned_hit
  }
  def getAssignmentsForHITId(hit_id: String) : List[Assignment] = {
    assignments_by_question_id(getQuestionIdForHITId(hit_id)).map { case (assn,_) => assn }
  }
  def getAssignmentsByAssignmentId : Map[String,(Assignment,AssignmentStatus.Value,String)] = {
    MockServiceStateUtils.getAssignmentsByAssignmentId(assignments_by_question_id)
  }
  def budgetDelta(delta: java.math.BigDecimal) : MockServiceState = {
    assert (delta.compareTo(budget) != 1)
    MockServiceState(budget.add(delta), assignments_by_question_id, hits, hit_type_ids, hitstates)
  }
  def getHITforHITId(hit_id: String) : HIT = {
    val matching_hits = hits.filter(_.getHITId == hit_id)
    assert(matching_hits.size == 1)
    matching_hits.head
  }

  private def getQuestionIdForHITId(hit_id: String) : String = {
    getHITforHITId(hit_id).getRequesterAnnotation
  }
}

object MockServiceStateUtils {
  def getAssignmentsByAssignmentId(assignments_by_question_id: Map[String, List[(Assignment, AssignmentStatus.Value)]])
    : Map[String,(Assignment,AssignmentStatus.Value,String)] = {
    assignments_by_question_id.flatMap { case (question_id, assns) =>
      assns.map { case (assn,assn_status) =>
        assn.getAssignmentId -> (assn, assn_status, question_id)
      }
    }
  }

  def changeAssignmentStatus(assignmentId: String, status: AssignmentStatus.Value, assn_map: Map[String,List[(Assignment, AssignmentStatus.Value)]])
  : Map[String,List[(Assignment, AssignmentStatus.Value)]] = {

    val assignments_by_a_id = MockServiceStateUtils.getAssignmentsByAssignmentId(assn_map)

    val (assn,assn_status,question_id) = assignments_by_a_id(assignmentId)

    // Ensure that only valid state transitions are allowed
    assert(
      status match {
        case AssignmentStatus.ACCEPTED => assn_status == AssignmentStatus.ACCEPTED
        case AssignmentStatus.REJECTED => assn_status == AssignmentStatus.ACCEPTED
        case AssignmentStatus.ANSWERED => assn_status == AssignmentStatus.UNANSWERED
        case _ => false
      }
    )

    val a_list = assn_map(question_id).filter { case (a,_) => a.getAssignmentId != assn.getAssignmentId }

    assn_map + (question_id -> ((assn, AssignmentStatus.REJECTED) :: a_list))
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

    // get cost for approvals
    val cost = assignmentIds.map { assn_id =>
      _state.getHITforHITId(_state.getAssignmentsByAssignmentId(assn_id)._1.getHITId).getReward.getAmount
    }.foldLeft(java.math.BigDecimal.ZERO){ (acc, bdnum) => acc.add(bdnum) }

    // get updated assignment map
    val assignments_by_question_id2 = assignmentIds.foldLeft(_state.assignments_by_question_id){ case (a_map, assn_id) =>
      MockServiceStateUtils.changeAssignmentStatus(assn_id, AssignmentStatus.ACCEPTED, a_map)
    }

    _state = _state.budgetDelta(cost.negate())
    _state = _state.updateAssignments(assignments_by_question_id2)
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
    val a_map = MockServiceStateUtils.changeAssignmentStatus(
                  assignmentId,
                  AssignmentStatus.REJECTED,
                  _state.assignments_by_question_id
                )

    _state = _state.updateAssignments(a_map)
  }

  override def getAllAssignmentsForHIT(hitId: String): Array[Assignment] = {
    _state.getAssignmentsForHITId(hitId).toArray
  }

  override def rejectQualificationRequest(qualificationRequestId: String,
                                          reason: String): Unit = {
    // NOP
  }

  override def getAccountBalance = {
    _state.budget.doubleValue()
  }

  override def revokeQualification(qualificationTypeId: String,
                                   subjectId: String,
                                   reason: String): Unit = {
    // NOP
  }

  override def disposeQualificationType(qualificationTypeId: String): QualificationType = {
    // we can only get away with this because I know that AutoMan does not
    // do anything with the returned QualificationType
    null
  }

  override def getAllQualificationRequests(qualificationTypeId: String): Array[QualificationRequest] = {
    Array[QualificationRequest]()
  }

  override def grantQualification(qualificationRequestId: String,
                                  integerValue: Integer): Unit = {
    // NOP
  }

  override def createQualificationType(name: String,
                                       keywords: String,
                                       description: String): QualificationType = {
    val qt = new QualificationType()
    qt.setName(name)
    qt.setKeywords(keywords)
    qt.setDescription(description)

    qt
  }

  override def assignQualification(qualificationTypeId: String,
                                   workerId: String,
                                   integerValue: Integer,
                                   sendNotification: Boolean): Unit = {
    // NOP
  }
}
