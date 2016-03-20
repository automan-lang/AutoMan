package edu.umass.cs.automan.adapters.mturk.worker

import java.text.SimpleDateFormat
import java.util.{UUID, Date}
import com.amazonaws.mturk.requester.{Assignment, Comparator, QualificationRequirement}
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.mock.MockRequesterService
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key._
import edu.umass.cs.automan.core.logging.{LogLevelDebug, LogType, LogLevelInfo, DebugLog}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import edu.umass.cs.automan.core.util.{Stopwatch, Utilities}

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

  private[worker] def mturk_grantQualifications(hitstate: HITState, state: MTState, backend: RequesterService) : MTState = {
    var internal_state = state

    // get all requests for this HIT's group disqualification
    val requests = backend.getAllQualificationRequests(
      hitstate.hittype.disqualification.getQualificationTypeId
    )

    requests.foreach { request =>
      // "SubjectId" === "WorkerId"
      val worker_id = request.getSubjectId

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
        if (internal_state.getHITTypeForWhitelistedWorker(worker_id, group_id).id != hit_type_id) {
          backend.rejectQualificationRequest(request.getQualificationRequestId,
            "You have already requested a qualification or submitted work for an associated HITType " +
              "that disqualifies you from participating in this HITType."
          )
          // otherwise, they're requesting something we've already granted; reject
        } else {
          backend.rejectQualificationRequest(request.getQualificationRequestId,
            "You cannot request this qualification more than once."
          )
        }
      } else {
        // if we don't know them, record the user and grant their disqualification request
        internal_state = internal_state.updateWorkerWhitelist(worker_id, group_id, hit_type_id)
        // get the BatchKey associated with the HITType; guaranteed to exist
        val batchKey = internal_state.getBatchKeyByHITTypeId(hit_type_id).get
        // get the batch_no associated with the BatchKey; guaranteed to exist
        backend.grantQualification(request.getQualificationRequestId, internal_state.getBatchNo(batchKey).get)
      }
    }

    internal_state
  }

  private[worker] def mturk_createQualification(title: String,
                                        batchKey: BatchKey,
                                        batch_no: Int,
                                        backend: RequesterService) : QualificationRequirement = {
    // get a simply-formatted date
    val sdf = new SimpleDateFormat("yyyy-MM-dd:z")
    val datestr = sdf.format(new Date())

    DebugLog("Creating disqualification.",LogLevelInfo(),LogType.ADAPTER,null)
    val qualtxt = s"AutoMan automatically generated Disqualification (title: $title, date: $datestr, batchKey: $batchKey, batch_no: $batch_no)"
    val qual = backend.createQualificationType("AutoMan " + UUID.randomUUID(), "automan", qualtxt)
    new QualificationRequirement(qual.getQualificationTypeId, Comparator.EqualTo, batch_no, null, false)
  }

  private[worker] def mturk_getAllAssignmentsForHIT(hit_state: HITState, backend: RequesterService): Array[Assignment] = {
    backend.getAllAssignmentsForHIT(hit_state.HITId)
  }

  private[worker] def mturk_approveAssignment(assignment: Assignment, text: String, backend: RequesterService) : Unit = {
    backend.approveAssignment(assignment.getAssignmentId, text)
  }

  private[worker] def mturk_rejectAssignment(assignment: Assignment, reason: String, backend: RequesterService) : Unit = {
    backend.rejectAssignment(assignment.getAssignmentId, reason)
  }

  private[worker] def mturk_forceExpireHIT(hit_state: HITState, backend: RequesterService) : Unit = {
    backend.forceExpireHIT(hit_state.HITId)
  }

  private[worker] def mturk_getAccountBalance(backend: RequesterService): BigDecimal = {
    backend.getAccountBalance
  }

  private[worker] def mturk_disposeQualificationType(qual: QualificationRequirement, backend: RequesterService) : Unit = {
    backend.disposeQualificationType(qual.getQualificationTypeId)
  }

  private[worker] def mturk_assignQualification(disqualification_id: String, worker_id: String, integerValue: Int, sendNotification: Boolean, backend: RequesterService): Unit = {
    backend.assignQualification(disqualification_id, worker_id, integerValue, sendNotification)
  }

  private[worker] def mturk_revokeQualification(qualificationTypeId: String, worker_id: String, reason: String, backend: RequesterService) : Unit = {
    backend.revokeQualification(qualificationTypeId, worker_id, reason)
  }


  /**
    * Create a new HITType on MTurk, with a disqualification if applicable.
    */
  private[worker] def mturk_registerHITType(title: String,
                                    desc: String,
                                    keywords: List[String],
                                    batch_key: BatchKey,
                                    state: MTState,
                                    backend: RequesterService) : MTState = {
    var internal_state = state

    DebugLog("Registering new HIT Type for batch key = " + batch_key, LogLevelDebug(), LogType.ADAPTER, null)

    val (group_id, cost, worker_timeout) = batch_key

    // update batch counter
    internal_state = internal_state.updateBatchNo(batch_key)

    // get just-created batch number; guaranteed to exist because we just created it
    val batch_no = internal_state.getBatchNo(batch_key).get

    // create disqualification for batch
    val disqualification = mturk_createQualification(title, batch_key, batch_no, backend)
    DebugLog(s"Created disqualification ${disqualification.getQualificationTypeId} for batch key = " + batch_key, LogLevelDebug(), LogType.ADAPTER, null)

    // whenever we create a new group, we need to add the disqualification to the HITType
    // EXCEPT if it's the very first time the group is posted
    val qs =
      if (batch_no != 1) {
        DebugLog(s"Batch #${batch_no} run, using disqualification ${disqualification.getQualificationTypeId} for batch " + batch_key, LogLevelDebug(), LogType.ADAPTER, null)
        List(disqualification)
      } else {
        DebugLog(s"Batch #${batch_no} run, using no qualifications for batch " + batch_key, LogLevelDebug(), LogType.ADAPTER, null)
        Nil
      }

    val autoApprovalDelayInSeconds = (3 * 24 * 60 * 60).toLong     // 3 days

    val hit_type_id = backend.registerHITType(
      autoApprovalDelayInSeconds,
      worker_timeout.toLong + WORKER_TIMEOUT_EPSILON_S,               // amount of time the worker has to complete the task
      cost.toDouble,                                                // cost in USD
      title,                                                        // title
      keywords.mkString(","),                                       // keywords
      desc,                                                         // description
      qs.toArray                                                    // qualifications
    )
    val hittype = HITType(hit_type_id, disqualification, group_id)

    // update disqualification map
    internal_state = internal_state.updateDisqualifications(disqualification.getQualificationTypeId, hittype.id)

    // update hittype map
    internal_state = internal_state.updateHITTypes(batch_key, hittype)

    internal_state
  }

  private[worker] def mturk_createHIT(ts: List[Task], batch_key: BatchKey, question: Question, state: MTState, backend: RequesterService) : MTState = {
    var internal_state = state

    // question
    val q = ts.head.question.asInstanceOf[MTurkQuestion]

    // get hit_type for batch
    val (hit_type,state2) = get_or_create_hittype(question.title, q.description, q.keywords, batch_key, internal_state, backend)
    internal_state = state2

    // render XML
    val xml = question.asInstanceOf[MTurkQuestion].toXML(randomize = true).toString()
    DebugLog("Posting task XML:\n" + xml.toString, LogLevelDebug(), LogType.ADAPTER, question.id)

    val hit = backend.createHIT(
      hit_type.id,                        // hitTypeId
      null,                               // title; defined by HITType
      null,                               // description
      null,                               // keywords; defined by HITType
      xml,                                // question xml
      null,                               // reward; defined by HITType
      null,                               // assignmentDurationInSeconds; defined by HITType
      null,                               // autoApprovalDelayInSeconds; defined by HITType
      ts.head.timeout_in_s.toLong,        // lifetimeInSeconds
      ts.size,                            // maxAssignments
      question.id.toString,               // requesterAnnotation
      Array[QualificationRequirement](),  // qualificationRequirements; defined by HITType
      Array[String]())                    // responseGroup
    // we immediately query the backend for the HIT's complete details
    // because the HIT structure returned by createHIT has a number
    // of uninitialized fields; return new HITState
    val hs = HITState(backend.getHIT(hit.getHITId), ts, hit_type)

    // calculate new HIT key
    val hit_key = (batch_key, question.memo_hash)

    DebugLog(s"Creating new HIT with ID ${hs.HITId} for batch key ${batch_key} and ${ts.size} assignments.", LogLevelDebug(), LogType.ADAPTER, question.id)

    // we update the state like this so that inconsistent state snapshots are not possible
    // update HIT key -> HIT ID map
    internal_state = internal_state.updateHITIDs(hit_key, hs.HITId)

    // update HIT ID -> HITState map
    internal_state.updateHITStates(hs.HITId, hs)
  }

  private[worker] def mturk_extendHIT(ts: List[Task], timeout_in_s: Int, hit_key: HITKey, state: MTState, backend: RequesterService) : MTState = {
    val internal_state = state

    val hitstate = internal_state.getHITState(hit_key)

    // MTurk does not allow expiration dates sooner than
    // 60 seconds into the future
    val expiry_s = Math.max(60, timeout_in_s).toLong

    DebugLog(s"Extending HIT ID ${hitstate.HITId} with ${ts.size} new assignments and timeout ${expiry_s} sec", LogLevelDebug(), LogType.ADAPTER, ts.head.question.id)

    // Note that extending HITs is only useful when the only
    // parameters that can change are the 1) number of assignments and
    // the 2) expiration date.
    backend.extendHIT(hitstate.HITId, ts.size, expiry_s)
    // we immediately query the backend for the HIT's complete details
    // to update our cached data

    // update HITState and return
    val hs = hitstate.addNewTasks(backend.getHIT(hitstate.HITId), ts)

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
  private[worker] def get_or_create_hittype(title: String, desc: String, keywords: List[String], batch_key: BatchKey, state: MTState, backend: RequesterService) : (HITType, MTState) = {
    var internal_state = state

    // when these properties change from what we've seen before
    // (including the possibility that we've never seen any of these
    // tasks before) we need to create a new HITType;
    // Note that simply adding blacklisted/excluded workers to an existing group
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
}
