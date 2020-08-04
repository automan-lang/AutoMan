package edu.umass.cs.automan.adapters.mturk.mock

import java.lang
import scala.collection.JavaConverters._
import com.amazonaws.{AmazonWebServiceRequest, ClientConfiguration, Request, ResponseMetadata}
import com.amazonaws.services.mturk.model._
import com.amazonaws.services.mturk.model.{Assignment, HIT, HITReviewStatus, HITStatus}
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.adapters.mturk.worker.WorkerRunnable
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.util._
import java.util.{Calendar, Date, UUID}

import com.amazonaws.services.mturk.AmazonMTurk
import com.amazonaws.services.mturk.model.{QualificationRequirement, ServiceException}

/**
 * An object used to simulate a Mechanical Turk backend. Can be used by
 * telling the MTurkAdapter to use_mock.  Mock answers should be provided
 * in an MTQuestion's init lambda with the mock_answers field. This object
 * should not be directly instantiated. All methods should be thread-safe
 * since this object's methods may be invoked by multiple threads.
 * @param initial_state a MockServiceState object representing the initial state.
 *  config an Amazon SDK ClientConfiguration object; not actually used.
 */

private[mturk] class MockRequesterService(initial_state: MockServiceState) extends AmazonMTurk {
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

  def forceExpireHIT(hitId: String): Unit = synchronized {
    // NOP
    diePeriodically()
  }

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
        .withQualificationRequirements(qualificationRequirements.toList.asJava)
        .withHITReviewStatus(HITReviewStatus.NotReviewed)
        .withNumberOfAssignmentsPending(0)
        .withNumberOfAssignmentsAvailable(maxAssignments)
        .withNumberOfAssignmentsCompleted(0)
    java.util.Arrays.asList(qualificationRequirements)
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

    def rejectAssignment(rejectAssignmentRequest: RejectAssignmentRequest): RejectAssignmentResult = synchronized {
    diePeriodically()
    _state = _state.updateAssignmentStatus(UUID.fromString(rejectAssignmentRequest.getAssignmentId), AssignmentStatus.REJECTED)
     new RejectAssignmentResult
  }

  def getAllAssignmentsForHIT(hitId: String): Array[Assignment] = synchronized {
    diePeriodically()
    val question_id = UUID.fromString(_state.getHITforHITId(hitId).getRequesterAnnotation)

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
        "Submitted",
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

  override def acceptQualificationRequest(acceptQualificationRequestRequest: AcceptQualificationRequestRequest): AcceptQualificationRequestResult = ???

  override def approveAssignment(approveAssignmentRequest: ApproveAssignmentRequest): ApproveAssignmentResult = ???

  override def associateQualificationWithWorker(associateQualificationWithWorkerRequest: AssociateQualificationWithWorkerRequest): AssociateQualificationWithWorkerResult = ???

  override def createAdditionalAssignmentsForHIT(createAdditionalAssignmentsForHITRequest: CreateAdditionalAssignmentsForHITRequest): CreateAdditionalAssignmentsForHITResult = ???

  override def createHIT(createHITRequest: CreateHITRequest): CreateHITResult = ???

  override def createHITType(createHITTypeRequest: CreateHITTypeRequest): CreateHITTypeResult = ???

  override def createHITWithHITType(createHITWithHITTypeRequest: CreateHITWithHITTypeRequest): CreateHITWithHITTypeResult = ???

  override def createQualificationType(createQualificationTypeRequest: CreateQualificationTypeRequest): CreateQualificationTypeResult = ???

  override def createWorkerBlock(createWorkerBlockRequest: CreateWorkerBlockRequest): CreateWorkerBlockResult = ???

  override def deleteHIT(deleteHITRequest: DeleteHITRequest): DeleteHITResult = ???

  override def deleteQualificationType(deleteQualificationTypeRequest: DeleteQualificationTypeRequest): DeleteQualificationTypeResult = ???

  override def deleteWorkerBlock(deleteWorkerBlockRequest: DeleteWorkerBlockRequest): DeleteWorkerBlockResult = ???

  override def disassociateQualificationFromWorker(disassociateQualificationFromWorkerRequest: DisassociateQualificationFromWorkerRequest): DisassociateQualificationFromWorkerResult = ???

  override def getAccountBalance(getAccountBalanceRequest: GetAccountBalanceRequest): GetAccountBalanceResult = {
    val res = new GetAccountBalanceResult()
    res.setAvailableBalance(initial_state.budget.toString)
    res
  }

  override def getAssignment(getAssignmentRequest: GetAssignmentRequest): GetAssignmentResult = ???

  override def getFileUploadURL(getFileUploadURLRequest: GetFileUploadURLRequest): GetFileUploadURLResult = ???

  override def getHIT(getHITRequest: GetHITRequest): GetHITResult = ???

  override def getQualificationScore(getQualificationScoreRequest: GetQualificationScoreRequest): GetQualificationScoreResult = ???

  override def getQualificationType(getQualificationTypeRequest: GetQualificationTypeRequest): GetQualificationTypeResult = ???

  override def listAssignmentsForHIT(listAssignmentsForHITRequest: ListAssignmentsForHITRequest): ListAssignmentsForHITResult = ???

  override def listBonusPayments(listBonusPaymentsRequest: ListBonusPaymentsRequest): ListBonusPaymentsResult = ???

  override def listHITs(listHITsRequest: ListHITsRequest): ListHITsResult = ???

  override def listHITsForQualificationType(listHITsForQualificationTypeRequest: ListHITsForQualificationTypeRequest): ListHITsForQualificationTypeResult = ???

  override def listQualificationRequests(listQualificationRequestsRequest: ListQualificationRequestsRequest): ListQualificationRequestsResult = ???

  override def listQualificationTypes(listQualificationTypesRequest: ListQualificationTypesRequest): ListQualificationTypesResult = ???

  override def listReviewPolicyResultsForHIT(listReviewPolicyResultsForHITRequest: ListReviewPolicyResultsForHITRequest): ListReviewPolicyResultsForHITResult = ???

  override def listReviewableHITs(listReviewableHITsRequest: ListReviewableHITsRequest): ListReviewableHITsResult = ???

  override def listWorkerBlocks(listWorkerBlocksRequest: ListWorkerBlocksRequest): ListWorkerBlocksResult = ???

  override def listWorkersWithQualificationType(listWorkersWithQualificationTypeRequest: ListWorkersWithQualificationTypeRequest): ListWorkersWithQualificationTypeResult = ???

  override def notifyWorkers(notifyWorkersRequest: NotifyWorkersRequest): NotifyWorkersResult = ???

  override def rejectQualificationRequest(rejectQualificationRequestRequest: RejectQualificationRequestRequest): RejectQualificationRequestResult = ???

  override def sendBonus(sendBonusRequest: SendBonusRequest): SendBonusResult = ???

  override def sendTestEventNotification(sendTestEventNotificationRequest: SendTestEventNotificationRequest): SendTestEventNotificationResult = ???

  override def updateExpirationForHIT(updateExpirationForHITRequest: UpdateExpirationForHITRequest): UpdateExpirationForHITResult = ???

  override def updateHITReviewStatus(updateHITReviewStatusRequest: UpdateHITReviewStatusRequest): UpdateHITReviewStatusResult = ???

  override def updateHITTypeOfHIT(updateHITTypeOfHITRequest: UpdateHITTypeOfHITRequest): UpdateHITTypeOfHITResult = ???

  override def updateNotificationSettings(updateNotificationSettingsRequest: UpdateNotificationSettingsRequest): UpdateNotificationSettingsResult = ???

  override def updateQualificationType(updateQualificationTypeRequest: UpdateQualificationTypeRequest): UpdateQualificationTypeResult = ???

  override def shutdown(): Unit = ???

  override def getCachedResponseMetadata(request: AmazonWebServiceRequest): ResponseMetadata = ???
}
