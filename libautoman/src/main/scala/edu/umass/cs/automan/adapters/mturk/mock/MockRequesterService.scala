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
import HITBackendStatus._
import AssignmentStatus._

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
        _state.answers_by_assignment_id(assn_id).toXML,
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

  def registerQuestion(question: Question): Unit = synchronized {
    val mtq = question.asInstanceOf[MTurkQuestion]
    val assignments = mtq.mock_answers.map { a => UUID.randomUUID() -> mtq.toMockResponse(question.id, a)}.toMap
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

  override def approveAssignment(assignmentId: String, requesterFeedback: String): Unit = {
    _state = _state.updateAssignmentStatus(UUID.fromString(assignmentId), AssignmentStatus.APPROVED)
  }
}
