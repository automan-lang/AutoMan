package edu.umass.cs.automan.adapters.mturk.mock

import java.lang
import java.lang.{Boolean, Double}
import com.amazonaws.mturk.addon.BatchItemCallback
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.ClientConfig
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.util._
import java.util.UUID

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

case class MockSetup(budget: BigDecimal)

case class MockHITType(id: UUID,
                       autoApprovalDelayInSeconds: lang.Long,
                       assignmentDurationInSeconds: lang.Long,
                       reward: Double, title: String,
                       keywords: String,
                       description: String,
                       qualRequirements: Array[QualificationRequirement])

case class MockServiceState(budget: java.math.BigDecimal,
                            questions_by_question_id: Map[UUID,Question[_]],
                            hit_type_by_hit_type_id: Map[String,MockHITType],
                            hits_by_question_id: Map[UUID, List[HIT]],
                            answers_by_assignment_id: Map[UUID,String],
                            assignment_status_by_assignment_id: Map[UUID,(AssignmentStatus.Value,Option[String])],
                            assignment_ids_by_question_id: Map[UUID, List[UUID]]) {
  def addHIT(question_id: UUID, hit: HIT) : MockServiceState = {
    val num = hit.getMaxAssignments

    // reserve the correct number of unreserved assignments for the hit
    val reserved_ids = assignment_ids_by_question_id(question_id).flatMap { a_id =>
      if (assignment_status_by_assignment_id(a_id)._1 == AssignmentStatus.UNANSWERED) {
        Some(a_id)
      } else {
        None
      }
    }.take(num)

    assert(reserved_ids.size == num)

    // update status map
    val status_map = reserved_ids.foldLeft(assignment_status_by_assignment_id) { case (acc, a_id) =>
      acc + (a_id -> (AssignmentStatus.ANSWERED, Some(hit.getHITId)))
    }

    // update hit list
    val hitlist = hit :: (
      if (hits_by_question_id.contains(question_id)) {
        hits_by_question_id(question_id)
      } else {
        List.empty
      })

    // return a new MockServiceState
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id + (question_id -> hitlist),
      answers_by_assignment_id,
      status_map,
      assignment_ids_by_question_id
    )
  }
  def addHITType(hit_type: MockHITType) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id + (hit_type.id.toString -> hit_type),
      hits_by_question_id,
      answers_by_assignment_id,
      assignment_status_by_assignment_id,
      assignment_ids_by_question_id
    )
  }
  def budgetDelta(delta: java.math.BigDecimal) : MockServiceState = {
    assert (delta.compareTo(budget) != 1)
    MockServiceState(
      budget.add(delta),
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      assignment_status_by_assignment_id,
      assignment_ids_by_question_id
    )
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
  def extendHIT(hitId: String, deltaSec: Int, deltaAssignments: Int) = {
    assert(hits_by_question_id.size >= 1)

    // get HIT
    val hit = getHITforHITId(hitId)
    val question_id = UUID.fromString(hit.getRequesterAnnotation)

    // clone
    val cloned_hit = cloneHIT(hit)

    // update selected fields
    cloned_hit.setExpiration(Utilities.calInSeconds(cloned_hit.getExpiration, deltaSec))
    cloned_hit.setMaxAssignments(cloned_hit.getMaxAssignments + deltaAssignments)

    // update hit list
    val hitlist = cloned_hit :: hits_by_question_id(question_id).filter(_.getHITId != hitId)

    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id + (question_id -> hitlist),
      answers_by_assignment_id,
      assignment_status_by_assignment_id,
      assignment_ids_by_question_id
    )
  }
  def getHITforHITId(hitId: String) : HIT = {
    assert(hits_by_question_id.size >= 1)
    hits_by_question_id.flatMap(_._2).filter(_.getHITId == hitId).head
  }
  def addQuestion(question: Question[_]) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id + (question.id -> question),
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      assignment_status_by_assignment_id,
      assignment_ids_by_question_id
    )
  }
  def addAssignments(question_id: UUID, assignments: Map[UUID,String]) : MockServiceState = {
    val answers = answers_by_assignment_id ++ assignments
    val status = assignment_status_by_assignment_id ++
                 assignments.map { case (id,a) => id -> (AssignmentStatus.UNANSWERED, None) }
    val a_by_q = assignment_ids_by_question_id + (question_id -> assignments.map(_._1).toList)
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers,
      status,
      a_by_q
    )
  }
  def updateAssignmentStatusMap(am: Map[UUID,(AssignmentStatus.Value,Option[String])]) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      am,
      assignment_ids_by_question_id
    )
  }
  def updateAssignmentStatus(assignmentId: UUID, new_status: AssignmentStatus.Value) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      changeAssignmentStatus(assignmentId, new_status, assignment_status_by_assignment_id),
      assignment_ids_by_question_id
    )
  }
  private def changeAssignmentStatus(assignmentId: UUID, new_status: AssignmentStatus.Value, assn_map: Map[UUID,(AssignmentStatus.Value,Option[String])])
  : Map[UUID,(AssignmentStatus.Value,Option[String])] = {
    val current_status = assn_map(assignmentId)._1

    // Ensure that only valid state transitions are allowed
    assert(
      new_status match {
        case AssignmentStatus.ACCEPTED => current_status == AssignmentStatus.ANSWERED
        case AssignmentStatus.REJECTED => current_status == AssignmentStatus.ANSWERED
        case AssignmentStatus.ANSWERED => current_status == AssignmentStatus.UNANSWERED
        case _ => false
      }
    )

    assn_map + (assignmentId -> (new_status, assn_map(assignmentId)._2))
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

}

/**
 * An object used to simulate a Mechanical Turk backend. Can be used by
 * telling the MTurkAdapter to use_mock.  Mock answers should be provided
 * in an MTQuestion's init lambda with the mock_answers field. This object
 * should not be directly instantiated. All methods should be thread-safe
 * since this object's methods may be invoked by multiple threads.
 * @param initial_state a MockServiceState object representing the initial state.
 * @param config an MTurk SDK ClientConfig object; not actually used.
 */
private[mturk] class MockRequesterService(initial_state: MockServiceState, config: ClientConfig) extends RequesterService(config) {
  var _state = initial_state

  override def approveAssignments(assignmentIds: Array[String],
                                  requesterFeedback: Array[String],
                                  defaultFeedback: String,
                                  callback: BatchItemCallback): Unit = synchronized {
    assert(assignmentIds.distinct.length == assignmentIds.length)
    assert(assignmentIds.length > 0)

    // convert to UUIDs
    val assn_ids = assignmentIds.map(UUID.fromString)

    // get cost for approvals
    val cost = assn_ids.map { assn_id =>
      val hit_id = _state.assignment_status_by_assignment_id(assn_id)._2.get
      _state.getHITforHITId(hit_id).getReward.getAmount
    }.foldLeft(java.math.BigDecimal.ZERO){ (acc, bdnum) => acc.add(bdnum) }

    // update budget
    _state = _state.budgetDelta(cost.negate())

    // update assignment status map
    _state = assn_ids.foldLeft(_state){ case (state, assn_id) =>
      state.updateAssignmentStatus(assn_id, AssignmentStatus.ACCEPTED)
    }
  }

  override def forceExpireHIT(hitId: String): Unit = synchronized {
    // NOP
  }

  override def createHIT(hitTypeId: String,
                         title: String,
                         description: String,
                         keywords: String,
                         question_xml: String,
                         reward: Double,
                         assignmentDurationInSeconds: lang.Long,
                         autoApprovalDelayInSeconds: lang.Long,
                         lifetimeInSeconds: lang.Long,
                         maxAssignments: Integer,
                         requesterAnnotation: String,
                         qualificationRequirements: Array[QualificationRequirement],
                         responseGroup: Array[String]): HIT = synchronized {
    val question_id = UUID.fromString(requesterAnnotation)
    val hit_id = UUID.randomUUID().toString
    val hit_type = _state.hit_type_by_hit_type_id(hitTypeId)

    val now = Utilities.nowCal()
    val expiry = Utilities.calInSeconds(now, lifetimeInSeconds.toInt)

    val hit = new HIT(
        null,                                       // request
        hit_id,                                     // HIT ID
        hitTypeId,                                  // HIT Type ID
        null,                                       // HIT Group ID
        null,                                       // HIT Layout ID
        now,                                        // creationTime
        hit_type.title,                             // title
        hit_type.description,                       // description
        question_xml,                               // question
        hit_type.keywords,                          // keywords
        HITStatus.Assignable,                       // HIT Status
        maxAssignments,                             // maxAssignments
        new Price(new java.math.BigDecimal(hit_type.reward), "USD", "$"),  // reward
        hit_type.autoApprovalDelayInSeconds,        // autoApprovalDelayInSeconds
        expiry,                                     // expiration
        hit_type.assignmentDurationInSeconds,       // assignmentDurationInSeconds
        requesterAnnotation,                        // requesterAnnotation
        hit_type.qualRequirements,                  // qualificationRequirements
        HITReviewStatus.NotReviewed,                // HITReviewStatus
        0,                                          // numberOfAssignmentsPending
        maxAssignments,                             // numberOfAssignmentsAvailable
        0                                           // numberOfAssignmentsCompleted
      )
    _state = _state.addHIT(question_id, hit)
    hit
  }

  override def getHIT(hitId: String): HIT = synchronized {
    _state.getHITforHITId(hitId)
  }

  override def extendHIT(hitId: String,
                         maxAssignmentsIncrement: Integer,
                         expirationIncrementInSeconds: lang.Long): Unit = synchronized {
    _state = _state.extendHIT(hitId, expirationIncrementInSeconds.toInt, maxAssignmentsIncrement)
  }

  override def rejectAssignment(assignmentId: String, requesterFeedback: String): Unit = synchronized {
    _state = _state.updateAssignmentStatus(UUID.fromString(assignmentId), AssignmentStatus.REJECTED)
  }

  private def answerToAssignment(answer: String) : String = {
    val assn =
      <Answer>
        &lt;QuestionFormAnswers&gt;
        { answer }
        &lt;/QuestionFormAnswers&gt;
      </Answer>
    assn.toString()
  }

  override def getAllAssignmentsForHIT(hitId: String): Array[Assignment] = synchronized {
    val question_id = UUID.fromString(_state.getHITforHITId(hitId).getRequesterAnnotation)

    val question = _state.questions_by_question_id(question_id).asInstanceOf[MTurkQuestion]

    val assn_ids = _state.assignment_status_by_assignment_id.filter { case (assn_id, (assn_stat, q_id_opt)) =>
        q_id_opt match {
          case Some(hit_id) => hit_id == hitId
          case None => false
        }
    }.map(_._1)

    assn_ids.map { assn_id =>
      new Assignment(
        null,
        assn_id.toString,
        UUID.randomUUID().toString,
        hitId,
        com.amazonaws.mturk.requester.AssignmentStatus.Submitted,
        Utilities.calInSeconds(Utilities.nowCal(), 16400),
        null,
        Utilities.nowCal(),
        null,
        null,
        null,
        answerToAssignment(_state.answers_by_assignment_id(assn_id)),
        null
      )
    }.toArray
  }

  override def rejectQualificationRequest(qualificationRequestId: String,
                                          reason: String): Unit = synchronized {
    // NOP
  }

  override def getAccountBalance = synchronized {
    _state.budget.doubleValue()
  }

  override def revokeQualification(qualificationTypeId: String,
                                   subjectId: String,
                                   reason: String): Unit = synchronized {
    // NOP
  }

  override def disposeQualificationType(qualificationTypeId: String): QualificationType = synchronized {
    // we can only get away with this because I know that AutoMan does not
    // do anything with the returned QualificationType
    null
  }

  override def getAllQualificationRequests(qualificationTypeId: String): Array[QualificationRequest] = synchronized {
    Array[QualificationRequest]()
  }

  override def grantQualification(qualificationRequestId: String,
                                  integerValue: Integer): Unit = synchronized {
    // NOP
  }

  override def createQualificationType(name: String,
                                       keywords: String,
                                       description: String): QualificationType = synchronized {
    val qt = new QualificationType()
    qt.setName(name)
    qt.setKeywords(keywords)
    qt.setDescription(description)

    qt
  }

  override def assignQualification(qualificationTypeId: String,
                                   workerId: String,
                                   integerValue: Integer,
                                   sendNotification: Boolean): Unit = synchronized {
    // NOP
  }

  def registerQuestion(question: Question[_]): Unit = synchronized {
    val mtq = question.asInstanceOf[MTurkQuestion]
    val assignments = mtq.mock_answers.map { a => UUID.randomUUID() -> mtq.answerToString(a)}.toMap
    _state = _state.addQuestion(question)
    _state = _state.addAssignments(question.id, assignments)
  }

  override def registerHITType(autoApprovalDelayInSeconds: lang.Long,
                               assignmentDurationInSeconds: lang.Long,
                               reward: Double,
                               title: String,
                               keywords: String,
                               description: String,
                               qualRequirements: Array[QualificationRequirement]): String = {
    val hit_type = MockHITType(
      UUID.randomUUID(),
      autoApprovalDelayInSeconds,
      assignmentDurationInSeconds,
      reward,
      title,
      keywords,
      description,
      qualRequirements
    )
    _state = _state.addHITType(hit_type)
    hit_type.id.toString
  }
}
