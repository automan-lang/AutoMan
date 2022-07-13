package org.automanlang.adapters.mturk.worker

import java.text.SimpleDateFormat
import java.util
import java.util.{Calendar, Date, UUID}

import scala.collection.JavaConverters._
import com.amazonaws.services.mturk.{AmazonMTurk, model}
import com.amazonaws.services.mturk.model._
import org.automanlang.adapters.mturk.util.ServiceExceptionRetry
import org.automanlang.adapters.mturk.question.MTurkQuestion
import org.automanlang.adapters.mturk.util.Key._
import org.automanlang.core.logging.{LogLevelDebug, LogType, LogLevelInfo, DebugLog}
import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.Task
import org.automanlang.core.util.Utilities.x_seconds_from_now
import scala.collection.JavaConversions._

/**
  * A collection of stateless MTurk utility functions.
  */
object MTurkMethods {
  // give workers a tiny amount of leeway since sometimes their computers are actually slow
  val WORKER_TIMEOUT_EPSILON_S = 15

  private[worker] def mtquestion_for_tasks(ts: List[Task]) : MTurkQuestion = {
    // determine which MT question we've been asked about
    question_for_tasks(ts) match {
      case mtq: MTurkQuestion => mtq
      case _ => throw new Exception("MTurkAdapter can only operate on Tasks for MTurkQuestions.")
    }
  }

  private[worker] def question_for_tasks(ts: List[Task]) : Question = {
    // determine which question we've been asked about
    val tg = ts.groupBy(_.question)
    if(tg.size != 1) {
      throw new Exception("MTurkAdapter can only process groups of Tasks for the same Question.")
    }
    tg.head._1
  }

  // Handle the user manual qualification requests
  private[worker] def mturk_grantQualifications(hitstate: HITState, state: MTState, backend: AmazonMTurk) : MTState = {
    var internal_state = state

    // get all requests for this HIT's group disqualification
    //val requests = backend.getAllQualificationRequests(
    val requests: util.List[model.QualificationRequest] = backend.listQualificationRequests( // requests is ListQualificationRequestsResult
      new ListQualificationRequestsRequest().withQualificationTypeId(hitstate.hittype.disqualification.getQualificationTypeId)
    ).getQualificationRequests() // now making list of QualificationRequests?
    //https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/mturk/model/QualificationRequest.html

    requests.foreach { request =>
      // "SubjectId" === "WorkerId"
      val worker_id = request.getWorkerId() //request.getSubjectId // worker id?

      // the HITType being requested
      val hit_type_id = if(internal_state.disqualifications.contains(request.getQualificationTypeId)) {
        internal_state.getHITTypeIDforQualificationTypeID(request.getQualificationTypeId)
      } else {
        throw new Exception("User-defined qualifications not yet supported.")
      }

      // the group_id for this HITType
      val group_id = hitstate.hittype.group_id

      // if the worker is known to us, then we've already granted them a disqualification
      if (internal_state.worker_whitelist.contains(worker_id, group_id)) {
        // if that disqualification is not the same as the one they're asking for, sorry, reject;
        // granting this would violate i.i.d. guarantee
        // note by Ye Shu: this is the case where disqualification has been granted for previous batch
        if (internal_state.getHITTypeForWhitelistedWorker(worker_id, group_id).id != hit_type_id) {
          backend.rejectQualificationRequest(new model.RejectQualificationRequestRequest()
            .withQualificationRequestId(request.getQualificationRequestId)
            .withReason("You have already requested a qualification or submitted work for an associated HITType " +
                        "that disqualifies you from participating in this HITType."))
          // otherwise, they're requesting something we've already granted; reject
        } else {
          // note by Ye Shu: this is the case where disqualification has been granted for this branch
          backend.rejectQualificationRequest(new model.RejectQualificationRequestRequest()
            .withQualificationRequestId(request.getQualificationRequestId)
            .withReason("You cannot request this qualification more than once.")
          )
        }
      } else {
        // if we don't know them, record the user and grant their disqualification request
        // note by Ye Shu: this code is unlikely to be reached, because we are now using qualification requirement
        // that "xxx has not been granted". So if a worker asks for qualification, they are likely already disqualified
        // However I'm keeping this code here just in case
        DebugLog(s"Granting qualification request: (worker ID: ${worker_id}, group ID: ${group_id}).", LogLevelInfo(), LogType.ADAPTER, null)

        internal_state = internal_state.updateWorkerWhitelist(worker_id, group_id, hit_type_id)
        // get the BatchKey associated with the HITType; guaranteed to exist
        val batchKey = internal_state.getBatchKeyByHITTypeId(hit_type_id).get
        // get the batch_no associated with the BatchKey; guaranteed to exist
        backend.acceptQualificationRequest(new AcceptQualificationRequestRequest()
          .withQualificationRequestId(request.getQualificationRequestId())
            .withIntegerValue(internal_state.getBatchNo(batchKey).get) // not sure if that's what's intended
        )
          //request.qualificationRequestId, internal_state.getBatchNo(batchKey).get)
      }
    }

    internal_state
  }

  /**
   * Creates `QualificationRequirement` for a question.
   *
   * This QualificationRequirement is then reused for the entire question.
   * It'll be set when a worker answers a question, so the worker will not be
   * able to answer the recreated question in future batches.
   */
  private[worker] def mturk_createQualification(title: String,
                                        batchKey: BatchKey,
                                        batch_no: Int,
                                        backend: AmazonMTurk): QualificationRequirement = {
    // get a simply-formatted date
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    val datestr = sdf.format(new Date())

    // Creates qualification type
    val qualID: QualificationID = {
      val qualtxt = s"""This disqualification ensures that you respond to our HIT only once. Thank you for your participation. (AutoMan title: \"$title\", date: $datestr, groupID: ${batchKey._1})"""

      val qual = backend.createQualificationType(new CreateQualificationTypeRequest()
        .withName(qualtxt)
        .withKeywords("automan")
        .withDescription(s"Automan ${UUID.randomUUID().toString}: $qualtxt")
        //.withQualificationTypeStatus(QualificationTypeStatus.Inactive)
        .withQualificationTypeStatus(QualificationTypeStatus.Active)
      ) : CreateQualificationTypeResult //UUID keyword?

      DebugLog(s"Created disqualification type ID: ${qual.getQualificationType.getQualificationTypeId}.",LogLevelInfo(),LogType.ADAPTER,null)
      qual.getQualificationType.getQualificationTypeId
    }

    // Creates Qualification Requirement
    new QualificationRequirement()
      .withQualificationTypeId(qualID)
      .withComparator(model.Comparator.DoesNotExist)
//      .withIntegerValues(batch_no)
      .withActionsGuarded("Accept")

    //model.Comparator.EqualTo, batch_no, null, false)
  }

  private[worker] def mturk_getAllAssignmentsForHIT(hit_state: HITState, backend: AmazonMTurk): Array[Assignment] = {
    val req: ListAssignmentsForHITRequest = new ListAssignmentsForHITRequest().withHITId(hit_state.HITId)
    var assns: Array[Assignment] = Array[Assignment]()
    var nextToken: String = null

    do {
      val res: ListAssignmentsForHITResult = backend.listAssignmentsForHIT(req)
      val iterAssns: Array[Assignment] = res.getAssignments().asScala.toArray[Assignment]
      assns = assns ++ iterAssns
      nextToken = res.getNextToken
      req.setNextToken(nextToken)
    } while(nextToken != null)

    assns
//    backend.listAssignmentsForHIT(new ListAssignmentsForHITRequest()
//      .withHITId(hit_state.HITId))
//      .getAssignments.asScala.toArray[Assignment]

    //val assignArr = assignList.toArray()[Assignment]//toArray
    //assignArr
  }

  private[worker] def mturk_approveAssignment(assignment: Assignment, text: String, backend: AmazonMTurk) : Unit = {
    backend.approveAssignment(new ApproveAssignmentRequest()
      .withAssignmentId(assignment.getAssignmentId)
      .withRequesterFeedback(text)
    )
    assignment.setApprovalTime(Calendar.getInstance.getTime)
//    backend.getAssignment(new GetAssignmentRequest()
//        .withAssignmentId()
//    )
  }

  private[worker] def mturk_rejectAssignment(assignment: Assignment, reason: String, backend: AmazonMTurk) : Unit = {
    backend.rejectAssignment(new RejectAssignmentRequest()
      .withAssignmentId(assignment.getAssignmentId) // ok if use non-sdk one...
      .withRequesterFeedback(reason))
    //(assignment.getAssignmentId, reason)
  }

  private[worker] def mturk_forceExpireHIT(hit_state: HITState, backend: AmazonMTurk) : Unit = {
    backend.updateExpirationForHIT(new UpdateExpirationForHITRequest()
    .withHITId(hit_state.HITId)
        .withExpireAt(new java.util.Date)) // need to do time in past?
  }

  private[worker] def mturk_getAccountBalance(backend: AmazonMTurk): BigDecimal = {
    val bal: String = backend.getAccountBalance(new GetAccountBalanceRequest).getAvailableBalance
    new java.math.BigDecimal(bal)
  }

  private[worker] def mturk_disposeQualificationType(qual_id: QualificationID, backend: AmazonMTurk) : Unit = {
    DebugLog(s"Deleting disqualification ID: ${qual_id}.",LogLevelInfo(),LogType.ADAPTER,null)
    // Does the qualification exist on the backend?  If so, delete.
    try {
      ServiceExceptionRetry (3) {
        // First check that the qualification exists on the backend; throws exception when qualification does not exist
        backend.getQualificationType(new GetQualificationTypeRequest().withQualificationTypeId((qual_id)))

        // No exception thrown, so delete
        backend.deleteQualificationType(new DeleteQualificationTypeRequest().withQualificationTypeId(qual_id))
      }
    } catch {
      // qualification does not exist; do nothing
      case e: RequestErrorException => ()
    }
  }

  private[worker] def mturk_assignQualification(disqualification_id: String, worker_id: String, integerValue: Int, sendNotification: Boolean, backend: AmazonMTurk): Unit = {
    backend.associateQualificationWithWorker(new AssociateQualificationWithWorkerRequest()
        .withQualificationTypeId(disqualification_id)
        .withWorkerId(worker_id)
        .withIntegerValue(integerValue)
        .withSendNotification(sendNotification)
    )
  }

  private[worker] def mturk_revokeQualification(qualificationTypeId: String, worker_id: String, reason: String, backend: AmazonMTurk) : Unit = {
    //backend.revokeQualification(qualificationTypeId, worker_id, reason)
    backend.disassociateQualificationFromWorker(new DisassociateQualificationFromWorkerRequest()
        .withQualificationTypeId(qualificationTypeId)
        .withWorkerId(worker_id)
        .withReason(reason)
    )
  }


  /**
    * Create a new HITType on MTurk, with a disqualification if applicable.
    */
  private[worker] def mturk_registerHITType(title: String,
                                    desc: String,
                                    keywords: List[String],
                                    batch_key: BatchKey,
                                    state: MTState,
                                    backend: AmazonMTurk) : MTState = {
    var internal_state = state

    DebugLog("Registering new HIT Type for batch key = " + batch_key, LogLevelDebug(), LogType.ADAPTER, null)

    val (group_id, cost, worker_timeout) = batch_key

    // update batch counter
    internal_state = internal_state.updateBatchNo(batch_key)

    // get just-created batch number; guaranteed to exist because we just created it
    val batch_no = internal_state.getBatchNo(batch_key).get

    // create disqualification for question
    // if already exists (i.e. not first batch) then reuse disqualification
    // This disqualification ensures that the question and its recreated version
    // (with longer worker timeouts) are only answered once maximum by each worker
    // TODO: tinker with imports
    val disqualification: QualificationRequirement =
    internal_state.qualificationRequirements.getOrElse(group_id, {
      val qual = mturk_createQualification(title, batch_key, batch_no, backend)
      DebugLog(s"""Created new disqualification with type ID ${qual.getQualificationTypeId} for group id \"${group_id}\"""", LogLevelInfo(), LogType.ADAPTER, null)
      // update qualifications so it can be reused for future batches
      internal_state = internal_state.addQualificationRequirement(group_id, qual)
      qual
    })
    // getQTId is in the doc...

    val autoApprovalDelayInSeconds = (3 * 24 * 60 * 60).toLong     // 3 days

    val hit_type_id = backend.createHITType(new CreateHITTypeRequest()
      .withAutoApprovalDelayInSeconds(autoApprovalDelayInSeconds)
      .withAssignmentDurationInSeconds(worker_timeout.toLong + WORKER_TIMEOUT_EPSILON_S)              // amount of time the worker has to complete the task
      .withReward(cost.toString())                                                // cost in USD TODO: reward == cost?
      .withTitle(title)                                                      // title
      .withKeywords(keywords.mkString(","))                              // keywords
      .withDescription(desc)                                                      // description
      .withQualificationRequirements(List(disqualification))                      // qualifications
    )
    val hittype = HITType(hit_type_id.getHITTypeId, disqualification, group_id)

    // update disqualification map
    internal_state = internal_state.addDisqualifications(disqualification.getQualificationTypeId, hittype.id)

    // update hittype map
    internal_state = internal_state.updateHITTypes(batch_key, hittype)

    internal_state
  }

  private[worker] def mturk_createHIT(ts: List[Task], batch_key: BatchKey, question: Question, state: MTState, backend: AmazonMTurk) : MTState = {
    var internal_state = state

    // question
    val q = ts.head.question.asInstanceOf[MTurkQuestion]

    // get hit_type for batch
    val (hit_type,state2) = get_or_create_hittype(question.title, q.description, q.keywords, batch_key, internal_state, backend)
    internal_state = state2

    // render HTML
    val html = question.asInstanceOf[MTurkQuestion].toHTML(randomize = true)
    DebugLog("Posting task HTML:\n" + html, LogLevelDebug(), LogType.ADAPTER, question.id)

    var hit = backend.createHITWithHITType(
      new CreateHITWithHITTypeRequest()
        .withHITTypeId(hit_type.id)
        .withQuestion(html)
        .withLifetimeInSeconds(ts.head.timeout_in_s.toLong)
        .withMaxAssignments(ts.size)
        .withRequesterAnnotation(question.id.toString)
    )

    // we immediately query the backend for the HIT's complete details
    // because the HIT structure returned by createHIT has a number
    // of uninitialized fields; return new HITState
    val hs = HITState(backend.getHIT(new GetHITRequest().withHITId(hit.getHIT.getHITId)).getHIT, ts, hit_type)

    // calculate new HIT key
    val hit_key = (batch_key, question.memo_hash)

    DebugLog(s"Creating new HIT with ID ${hs.HITId} for batch key ${batch_key} and ${ts.size} assignments.", LogLevelDebug(), LogType.ADAPTER, question.id)

    // we update the state like this so that inconsistent state snapshots are not possible
    // update HIT key -> HIT ID map
    internal_state = internal_state.updateHITIDs(hit_key, hs.HITId)

    // update HIT ID -> HITState map
    internal_state.updateHITStates(hs.HITId, hs)
  }

  private[worker] def mturk_extendHIT(ts: List[Task], timeout_in_s: Int, hit_key: HITKey, state: MTState, backend: AmazonMTurk) : MTState = {
    val internal_state = state

    val hitstate = internal_state.getHITState(hit_key)

    // MTurk does not allow expiration dates sooner than
    // 60 seconds into the future
    val expiry_s = Math.max(60, timeout_in_s)

    DebugLog(s"Extending HIT ID ${hitstate.HITId} with ${ts.size} new assignments and timeout ${expiry_s} sec", LogLevelDebug(), LogType.ADAPTER, ts.head.question.id)

    // Note that extending HITs is only useful when the only
    // parameters that can change are the 1) number of assignments and
    // the 2) expiration date.
    if (ts.nonEmpty) {
      backend.createAdditionalAssignmentsForHIT(new CreateAdditionalAssignmentsForHITRequest()
        .withHITId(hitstate.HITId)
        .withNumberOfAdditionalAssignments(ts.size))
    }
    backend.updateExpirationForHIT(new UpdateExpirationForHITRequest()
      .withHITId(hitstate.HITId)
      .withExpireAt(x_seconds_from_now(expiry_s)))

    // we immediately query the backend for the HIT's complete details
    // to update our cached data

    // update HITState and return
    val hs = hitstate.addNewTasks(backend.getHIT(new GetHITRequest().withHITId(hitstate.HITId)).getHIT, ts)

    // update hit states with new object
    internal_state.updateHITStates(hs.HITId, hs)
  }

  /**
    * Checks that a HITType already exists for the task group;
    * if it does, it returns the associated HITType object,
    * otherwise it creates a HITType on MTurk.
    * @param batch_key A GroupKey tuple that uniquely identifies a batch round.
    * @return A HITType
    */
  private[worker] def get_or_create_hittype(title: String, desc: String, keywords: List[String], batch_key: BatchKey, state: MTState, backend: AmazonMTurk) : (HITType, MTState) = {
    var internal_state = state

    // when these properties change from what we've seen before
    // (including the possibility that we've never seen any of these
    // tasks before) we need to create a new HITType;
    // Note that simply adding banned/excluded workers to an existing group
    // is not sufficient to trigger the creation of a new HITType, nor do we want
    // it to, because MTurk's extendHIT is sufficient to prevent re-participation
    // for a given HIT.
    val (group_id, _, _) = batch_key

    if (!internal_state.hit_types.contains(batch_key)) {
      // request new HITTypeId from MTurk
      internal_state = mturk_registerHITType(title, desc, keywords, batch_key, internal_state, backend)
    } else {
      DebugLog(s"Reusing HITType with ID ${internal_state.hit_types(batch_key).id} for batch key ${batch_key}.", LogLevelInfo(), LogType.ADAPTER, null)
    }
    (internal_state.hit_types(batch_key), internal_state)
  }

  private[automanlang] def mturk_searchAllHITs(backend: AmazonMTurk) : ListHITsResult = {
    backend.listHITs(new ListHITsRequest())
  }
}
