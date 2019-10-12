package edu.umass.cs.automan.adapters.mturk.mock

import java.{lang, util}
import java.lang.{Boolean, Double}
import scala.collection.JavaConverters._

import com.amazonaws.Request
import com.amazonaws.services.mturk.{AmazonMTurk, model}
import com.amazonaws.services.mturk.model.{AssignmentStatus, QualificationRequest, QualificationType}
//import edu.umass.cs.automan.adapters.mturk.mock.AssignmentStatus.AssignmentStatus //TODO: change this?

//import com.amazonaws.Request
import com.amazonaws.services.mturk.model.{Assignment, HIT, HITReviewStatus, HITStatus}

//import com.amazonaws.mturk.requester._
//import com.amazonaws.mturk.service.axis.RequesterService
//import com.amazonaws.mturk.service.exception.ServiceException
//import com.amazonaws.mturk.util.ClientConfig
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.adapters.mturk.worker.WorkerRunnable
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.util._
import java.util.{Calendar, Date, UUID}

import com.amazonaws.services.mturk.AmazonMTurk
import com.amazonaws.services.mturk.model.{QualificationRequirement, ServiceException}
import com.sun.deploy.config.ClientConfig

/**
 * An object used to simulate a Mechanical Turk backend. Can be used by
 * telling the MTurkAdapter to use_mock.  Mock answers should be provided
 * in an MTQuestion's init lambda with the mock_answers field. This object
 * should not be directly instantiated. All methods should be thread-safe
 * since this object's methods may be invoked by multiple threads.
 * @param initial_state a MockServiceState object representing the initial state.
 * @param config an MTurk SDK ClientConfig object; not actually used.
 */
private[mturk] abstract class MockRequesterService(initial_state: MockServiceState, config: ClientConfig) extends AmazonMTurk { //TODO: what about config? (originally RequestorService(config))
  private var _state = initial_state
  private var _transaction_count = 0
  private val TRANSACTION_THRESHOLD = WorkerRunnable.OK_THRESHOLD + 1

  private def diePeriodically() : Unit = {
    _transaction_count += 1
    if (_transaction_count > TRANSACTION_THRESHOLD) {
      _transaction_count = 0
      throw new ServiceException("The MTurk SDK throws exceptions on service unavailable failures.")
    }
  }

//  override def forceExpireHIT(hitId: String): Unit = synchronized {
//    // NOP
//    diePeriodically()
//  }

  def createHIT(hitTypeId: String,
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
    diePeriodically()
    val question_id = UUID.fromString(requesterAnnotation)
    val hit_id = UUID.randomUUID().toString
    val hit_type = _state.hit_type_by_hit_type_id(hitTypeId)

    val now = Utilities.nowCal()
    val expiry = Utilities.calInSeconds(now, lifetimeInSeconds.toInt)

    val hit = new HIT()
        .withHITId(hit_id)
        .withHITTypeId(hitTypeId)
        .withCreationTime(now.getTime)
        .withTitle(hit_type.title)
        .withDescription(hit_type.description)
        .withQuestion(question_xml)
        .withKeywords(hit_type.keywords)
        .withHITStatus(HITStatus.Assignable)
        .withMaxAssignments(maxAssignments)
        .withReward(hit_type.reward.toString)
        .withAutoApprovalDelayInSeconds(hit_type.autoApprovalDelayInSeconds)
        .withExpiration(expiry.getTime)
        .withAssignmentDurationInSeconds(hit_type.assignmentDurationInSeconds)
        .withRequesterAnnotation(requesterAnnotation)
        .withQualificationRequirements(qualificationRequirements.toList.asJava)//java.util.Arrays.asList(qualificationRequirements))//(qualificationRequirements.toList)//(new util.LinkedList[QualificationRequirement]().addAll(0, hit_type.qualRequirements)) //hit_type.qualRequirements.toList)) //TODO: wut
        .withHITReviewStatus(HITReviewStatus.NotReviewed)
        .withNumberOfAssignmentsPending(0)
        .withNumberOfAssignmentsAvailable(maxAssignments)
        .withNumberOfAssignmentsCompleted(0)
    java.util.Arrays.asList(qualificationRequirements)
//        null,                                       // request
//        hit_id,                                     // HIT ID
//        hitTypeId,                                  // HIT Type ID
//        null,                                       // HIT Group ID
//        null,                                       // HIT Layout ID
//        now,                                        // creationTime
//        hit_type.title,                             // title
//        hit_type.description,                       // description
//        question_xml,                               // question
//        hit_type.keywords,                          // keywords
//        HITStatus.Assignable,                       // HIT Status
//        maxAssignments,                             // maxAssignments
//        new Price(new java.math.BigDecimal(hit_type.reward), "USD", "$"),  // reward
//        hit_type.autoApprovalDelayInSeconds,        // autoApprovalDelayInSeconds
//        expiry,                                     // expiration
//        hit_type.assignmentDurationInSeconds,       // assignmentDurationInSeconds
//        requesterAnnotation,                        // requesterAnnotation
//        hit_type.qualRequirements,                  // qualificationRequirements
//        HITReviewStatus.NotReviewed,                // HITReviewStatus
//        0,                                          // numberOfAssignmentsPending
//        maxAssignments,                             // numberOfAssignmentsAvailable
//        0                                           // numberOfAssignmentsCompleted
//      )
    _state = _state.addHIT(question_id, hit)
    hit
  }

  def getHIT(hitId: String): HIT = synchronized { // removed override
    diePeriodically()
    _state.getHITforHITId(hitId)
  }

  def extendHIT(hitId: String,
                         maxAssignmentsIncrement: Integer,
                         expirationIncrementInSeconds: lang.Long): Unit = synchronized {
    diePeriodically()
    _state = _state.extendHIT(hitId, expirationIncrementInSeconds.toInt, maxAssignmentsIncrement)
  }

  def rejectAssignment(assignmentId: String, requesterFeedback: String): Unit = synchronized {
    diePeriodically()
    _state = _state.updateAssignmentStatus(UUID.fromString(assignmentId), AssignmentStatus.REJECTED)
  }

  def getAllAssignmentsForHIT(hitId: String): Array[Assignment] = synchronized {
    diePeriodically()
    val question_id = UUID.fromString(_state.getHITforHITId(hitId).getRequesterAnnotation)

    val question = _state.question_by_question_id(question_id).asInstanceOf[MTurkQuestion]

    // grab our assignments
    val assn_ids2: List[UUID] =
      _state.assignment_ids_by_question_id(question_id).filter { assn_id =>
        _state.assignment_status_by_assignment_id(assn_id)._1 == AssignmentStatus.UNANSWERED
      }

    // duplicate a couple of assignments to ensure that duplicate
    // message handling works properly; MTurk occasionally resend
    // assignments
    val assn_ids = if (assn_ids2.nonEmpty) {
      assn_ids2.head :: (if (assn_ids2.tail.nonEmpty) List(assn_ids2.tail.head) else List()) ::: assn_ids2
    } else {
      assn_ids2
    }

    assn_ids.map { assn_id =>
      val mock_response = _state.answers_by_assignment_id(assn_id)

      newAssignment(
        null,
        assn_id.toString,
        mock_response.workerId.toString,
        hitId,
        //com.amazonaws.mturk.requester.AssignmentStatus.Submitted,
        "Submitted",
        //AssignmentStatus.withName("Submitted"), //TODO: how to set status?
        Utilities.calInSeconds(mock_response.responseTime, 16400),
        null,
        mock_response.responseTime,
        null,
        null,
        null,
        mock_response.toXML,
        null
      )
    }.toArray.sortBy(_.getSubmitTime)
  }

  /**
   * This method exists only because the SDK has meaningless
   * constructor parameter names and this one does not.
 *
   * @return Assignment object.
   */
  private def newAssignment(request: Request[Any], //TODO: figure out what type this should actually be
                             assignmentId: String,
                             workerId: String,
                             HITId: String,
                             assignmentStatus: String, // change back to AssignmentStatus.Value?
                             autoApprovalTime: Calendar,
                             acceptTime: Calendar,
                             submitTime: Calendar,
                             approvalTime: Calendar,
                             rejectionTime: Calendar,
                             deadline: Calendar,
                             answer: String,
                             requesterFeedback: String) : Assignment = {
    new Assignment()
      .withAssignmentId(assignmentId)
      .withWorkerId(workerId)
      .withHITId(HITId)
      .withAssignmentStatus(assignmentStatus: String)
      .withAutoApprovalTime(autoApprovalTime.getTime)
      .withAcceptTime(acceptTime.getTime)
      .withSubmitTime(submitTime.getTime)
      .withRejectionTime(rejectionTime.getTime)
      .withDeadline(deadline.getTime)
      .withAnswer(answer)
      .withRequesterFeedback(requesterFeedback)
//      request,
//      assignmentId,
//      workerId,
//      HITId,
//      assignmentStatus,
//      autoApprovalTime,
//      acceptTime,
//      submitTime,
//      approvalTime,
//      rejectionTime,
//      deadline,
//      answer,
//      requesterFeedback
  }

  def rejectQualificationRequest(qualificationRequestId: String,
                                          reason: String): Unit = synchronized {
    // NOP
    diePeriodically()
  }

  def getAccountBalance = synchronized {
    diePeriodically()
    _state.budget.doubleValue()
  }

  def revokeQualification(qualificationTypeId: String,
                                   subjectId: String,
                                   reason: String): Unit = synchronized {
    // NOP
    diePeriodically()
  }

  def disposeQualificationType(qualificationTypeId: String): QualificationType = synchronized {
    diePeriodically()
    val (mss,qt) = _state.deleteQualificationById(qualificationTypeId)
    _state = mss
    qt
  }

  def getAllQualificationRequests(qualificationTypeId: String): Array[QualificationRequest] = synchronized {
    diePeriodically()
    Array[QualificationRequest]()
  }

  def grantQualification(qualificationRequestId: String,
                                  integerValue: Integer): Unit = synchronized {
    diePeriodically()
    // NOP
  }

  def createQualificationType(name: String,
                                       keywords: String,
                                       description: String): QualificationType = synchronized {
    diePeriodically()
    val qt = new QualificationType()
    qt.setQualificationTypeId(UUID.randomUUID().toString)
    qt.setName(name)
    qt.setKeywords(keywords)
    qt.setDescription(description)

    _state = _state.addQualificationType(qt)

    qt
  }

  def assignQualification(qualificationTypeId: String,
                                   workerId: String,
                                   integerValue: Integer,
                                   sendNotification: Boolean): Unit = synchronized {
    // NOP
    diePeriodically()
  }

  /**
   * Register a question with the mock backend, and map assignment IDs
   * to the supplied mock answers.
 *
   * @param q Question.
   * @param t Scheduler startup time.
   */
  def registerQuestion(q: Question, t: Date): Unit = synchronized {
    val assignments = q.mock_answers.map { a =>
      // get time x milliseconds from t
      val d = Utilities.xMillisecondsFromDate(a.time_delta_in_ms, t)
      // pair with random assignment IDs
      UUID.randomUUID() -> q.toMockResponse(q.id, d, a.answer, a.worker_id)
    }.toMap
    // add question to mock state
    _state = _state.addQuestion(q)
    // add assignment map to question in mock state
    _state = _state.addAssignments(q.id, assignments)
  }

  def registerHITType(autoApprovalDelayInSeconds: lang.Long,
                               assignmentDurationInSeconds: lang.Long,
                               reward: Double,
                               title: String,
                               keywords: String,
                               description: String,
                               qualRequirements: Array[QualificationRequirement]): String = synchronized {
    diePeriodically()
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

  def approveAssignment(assignmentId: String, requesterFeedback: String): Unit = synchronized {
    diePeriodically()
    _state = _state.updateAssignmentStatus(UUID.fromString(assignmentId), AssignmentStatus.APPROVED)
  }

  def searchAllHITs(): Array[HIT] = synchronized {
    diePeriodically()
    _state.hits_by_question_id.flatMap(_._2).toArray
  }

  def takeAssignment(assignmentId: String): Unit = synchronized {
    _state = _state.updateAssignmentStatus(UUID.fromString(assignmentId), AssignmentStatus.ANSWERED)
  }

  def getAllQualificationTypes: Array[QualificationType] = {
    _state.qualification_types.toArray
  }
}
