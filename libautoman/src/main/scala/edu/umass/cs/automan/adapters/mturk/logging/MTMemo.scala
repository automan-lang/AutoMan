package edu.umass.cs.automan.adapters.mturk.logging

import java.util.{Calendar, UUID}

import com.amazonaws.services.mturk.model.{Assignment, Comparator, GetHITRequest, GetQualificationTypeRequest, QualificationRequirement}

//import com.amazonaws.mturk.requester._
//import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.services.mturk.AmazonMTurk
import com.amazonaws.services.mturk.model.{AssignmentStatus}
import edu.umass.cs.automan.adapters.mturk.worker.{HITState, HITType, MTState}
import edu.umass.cs.automan.adapters.mturk.logging.tables.{DBAssignment, DBQualificationRequirement}
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key._
import edu.umass.cs.automan.core.logging._

import scala.slick.driver.H2Driver.simple._

class MTMemo(log_config: LogConfig.Value, database_path: String, in_mem_db: Boolean) extends Memo(log_config, database_path, in_mem_db) {
  case class Assn(assignmentId: String,
                  workerId: String,
                  HITId: String,
                  assignmentStatus: AssignmentStatus,
                  autoApprovalTime: Option[Calendar],
                  acceptTime: Option[Calendar],
                  submitTime: Option[Calendar],
                  approvalTime: Option[Calendar],
                  rejectionTime: Option[Calendar],
                  deadline: Option[Calendar],
                  answer: String,
                  requesterFeedback: Option[String],
                  taskId: UUID) {
    def this(a: AssnTuple) = this(a._1, a._2, a._3, a._4, a._5, a._6, a._7, a._8, a._9, a._10, a._11, a._12, a._13)
    def toAssignment() : Assignment = {
      new Assignment().withAssignmentId(this.assignmentId)
        .withWorkerId(this.workerId)
        .withHITId(this.HITId)
        .withAssignmentStatus(this.assignmentStatus)
        .withAutoApprovalTime(this.autoApprovalTime.orNull.getTime())
        .withAcceptTime(this.acceptTime.orNull)
        .withSubmitTime(this.submitTime.orNull)
        .withApprovalTime(this.approvalTime.orNull)
        .withRejectionTime(this.rejectionTime.orNull)
        .withDeadline(this.deadline.orNull)
        .withAnswer(this.answer)
        .withRequesterFeedback(this.requesterFeedback.orNull)
//        null,
//        this.assignmentId,
//        this.workerId,
//        this.HITId,
//        this.assignmentStatus,
//        this.autoApprovalTime.orNull,
//        this.acceptTime.orNull,
//        this.submitTime.orNull,
//        this.approvalTime.orNull,
//        this.rejectionTime.orNull,
//        this.deadline.orNull,
//        this.answer,
//        this.requesterFeedback.orNull
//      )
    }
  }

  type AssnTuple = (String, String, String, AssignmentStatus, Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], String, Option[String], UUID)

  type DBHITType = (String, String, Int)
  type DBQualificationRequirement = (String, String)

  // TableQuery aliases
  private val dbAssignment = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBAssignment]
  private val dbHIT = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBHIT]
  private val dbHITType = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBHITType]
  private val dbQualReq = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBQualificationRequirement]
  private val dbTaskHIT = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBTaskHIT]
  private val dbWorker = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBWorker]

  override protected[automan] def init() : Unit = {
    val ddls = List(dbAssignment.ddl, dbHIT.ddl, dbHITType.ddl, dbQualReq.ddl, dbTaskHIT.ddl, dbWorker.ddl)
    init_database_if_required(ddls)
  }

  def save_mt_state(state: MTState) : Unit = {
    db_opt match {
      case Some(db) => db withSession { implicit s =>
        updateHITTypes(state.hit_types, state.batch_no)
        updateHITs(state.hit_states, state.hit_ids)
        updateWhitelist(state.worker_whitelist)
      }
      case None => ()
    }
  }

  def restore_mt_state(backend: AmazonMTurk) : Option[MTState] = {
    db_opt match {
      case Some(db) => db withSession { implicit s =>
        val hit_types: Map[Key.BatchKey, HITType] = getHITTypeMap
        val id_hittypes: Map[String, HITType] = getHITTypesByHITTypeId(hit_types)
        val hit_states: Map[String, HITState] = getHITStateMap(id_hittypes, backend)
        val hit_ids: Map[Key.HITKey, Key.HITID] = getHITIDMap

        Some(
          MTState(
            hit_types,
            hit_states,
            hit_ids,
            getWorkerWhitelist,
            getQualifications,
            getBatchNos
          )
        )
      }
      case None => None
    }
  }

  private def updateWhitelist(ww: Map[(WorkerID,GroupID),HITTypeID])(implicit session: DBSession) : Unit = {
    val existing_whitelist = getWorkerWhitelist
    val inserts = ww.flatMap { case ((worker_id,group_id),hit_type_id) =>
      if (existing_whitelist.contains((worker_id, group_id))) {
        None
      } else {
        Some((worker_id, group_id,hit_type_id))
      }
    }

    // worker whitelist inserts (no updates)
    dbWorker ++= inserts
  }

  private def getHITInsertsAndUpdates(existing_hits: Map[String, (String, String, Boolean)], hit_states: Map[HITID,HITState])
    : (List[HITID], List[HITID]) = {

    hit_states.values.map { hitstate =>
      if (existing_hits.contains(hitstate.HITId)) {
        if (existing_hits(hitstate.HITId)._3 != hitstate.isCancelled) {
          Update(hitstate.HITId)
        } else {
          Skip(hitstate.HITId)
        }
      } else {
        Insert(hitstate.HITId)
      }
    }.foldLeft(List.empty[HITID], List.empty[HITID]){ case (acc, action) =>
      action match {
        case Insert(hitid) => (hitid :: acc._1, acc._2)
        case Update(hitid) => (acc._1, hitid :: acc._2)
        case Skip(hitid) => acc
      }
    }
  }

  private def getAssignmentInsertsAndUpdates(existing_assignments: Map[String, Assn], hit_states: Map[HITID,HITState])
    : (List[(Assignment, UUID)], List[(Assignment, UUID)]) = {

    hit_states.values.flatMap { hitstate =>
      hitstate.t_a_map.flatMap { case (task_id, assignment_opt) =>
        assignment_opt match {
          case Some(assignment) =>
            if (existing_assignments.contains(assignment.getAssignmentId)) {
              val existing_assn = existing_assignments(assignment.getAssignmentId).toAssignment()
              if (existing_assn.equals(assignment)) {
                Some(Skip(assignment,task_id))
              } else {
                Some(Update(assignment,task_id))
              }
            } else {
              Some(Insert(assignment,task_id))
            }
          case None => None
        }
      }
    }.foldLeft(List.empty[(Assignment,UUID)],List.empty[(Assignment,UUID)]) { case (acc,action) =>
      action match {
        case Insert(data) => (data :: acc._1, acc._2)
        case Update(data) => (acc._1, data :: acc._2)
        case Skip(data) => acc
      }
    }
  }

  private def updateHITs(hit_states: Map[HITID,HITState], hit_ids: Map[HITKey,HITID])(implicit session: DBSession) : Unit = {
    implicit val statusMapper = DBAssignment.statusMapper

    val ah = allHITs.list
    val aa = allAssignments.list.map(new Assn(_))

    val existing_hits = ah.map { case (hit_id, hit_type_id, is_cancelled) => hit_id -> (hit_id, hit_type_id, is_cancelled) }.toMap
    val existing_assignments = aa.map { assn => assn.assignmentId -> assn }.toMap

    // if these sizes are different, then we are losing HITs or assignments when we make a map
    assert(ah.size == existing_hits.size)
    assert(aa.size == existing_assignments.size)

    // HITs
    val (hit_inserts: List[HITID], hit_updates: List[HITID]) = getHITInsertsAndUpdates(existing_hits, hit_states)

    // lists should only contain distinct elements & be completely disjoint
    assert(hit_inserts.distinct.length == hit_inserts.length)
    assert(hit_updates.distinct.length == hit_updates.length)
    assert((hit_inserts ::: hit_updates).distinct.length == hit_inserts.length + hit_updates.length)

    // Assignments
    val (assignment_inserts, assignment_updates) = getAssignmentInsertsAndUpdates(existing_assignments, hit_states)

    // lists should only contain distinct elements & be completely disjoint
    assert(assignment_inserts.map(_._1.getAssignmentId).distinct.length == assignment_inserts.length)
    assert(assignment_updates.map(_._1.getAssignmentId).distinct.length == assignment_updates.length)
    assert((assignment_inserts ::: assignment_updates).distinct.length == assignment_inserts.length + assignment_updates.length)

    // insert new HITs
    dbHIT ++= HITState2HITTuples(hit_inserts.map { hitid => hit_states(hitid)} )

    // mark cancelled HITs as cancelled in the database
    hit_updates.foreach { hit_id => dbHIT.filter(_.HITId === hit_id).map(_.isCancelled).update(hit_states(hit_id).isCancelled)}

    // insert TaskHITs (updates never needed)
    dbTaskHIT ++= HITState2TaskHITTuples(hit_inserts.map { hitid => hit_states(hitid)})

    // insert new Assignments
    val a_inserts = Assignment2AssignmentTuple(assignment_inserts)
    dbAssignment ++= a_inserts

    // update existing Assignments
    // a.getAssignmentStatus, a.getAutoApprovalTime, a.getAcceptTime, a.getSubmitTime, a.getApprovalTime, a.getRejectionTime, a.getDeadline, a.getRequesterFeedback
    assignment_updates.foreach { case (a, task_id) =>
      dbAssignment
        .filter(_.assignmentId === a.getAssignmentId)
        .map{ r => (r.assignmentStatus, r.autoApprovalTime, r.acceptTime, r.submitTime, r.approvalTime, r.rejectionTime, r.deadline, r.requesterFeedback) }
        .update(a.getAssignmentStatus, Option(a.getAutoApprovalTime), Option(a.getAcceptTime), Option(a.getSubmitTime), Option(a.getApprovalTime), Option(a.getRejectionTime), Option(a.getDeadline), Option(a.getRequesterFeedback)) //TODO: Not sure what this is
    }
  }

  private def Assignment2AssignmentTuple(pairs: List[(Assignment,UUID)]) : List[(String, String, String, AssignmentStatus, Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], String, Option[String], UUID)] = {
    pairs.map { case (assignment, task_id) =>
      (
        assignment.getAssignmentId,
        assignment.getWorkerId,
        assignment.getHITId,
        assignment.getAssignmentStatus,
        Option(assignment.getAutoApprovalTime),
        Option(assignment.getAcceptTime),
        Option(assignment.getSubmitTime),
        Option(assignment.getApprovalTime),
        Option(assignment.getRejectionTime),
        Option(assignment.getDeadline),
        assignment.getAnswer,
        Option(assignment.getRequesterFeedback),
        task_id
      )
    }
  }

  private def HITState2TaskHITTuples(hitstates: List[HITState]) : List[(String, UUID)] = {
    hitstates.flatMap { hitstate =>
      hitstate.t_a_map.keys.map { task_id => (hitstate.hit.getHITId, task_id) }
    }
  }

  private def HITState2HITTuples(hitstates: List[HITState]) : List[(String, String, Boolean)] = {
    hitstates.map { hitstate => (hitstate.HITId, hitstate.hittype.id, hitstate.isCancelled) }
  }

  // HITTypes and Qualifications never need updating; they are insert-only
  private def updateHITTypes(hittypes_by_batchkey: Map[BatchKey,HITType], batch_no: Map[GroupID, Map[BatchKey,Int]])(implicit session: DBSession) : Unit = {
    val existing_batches: Map[HITTypeID, Int] = allHITTypes.map { r => (r._1, r._5) }.list.toMap
    val batchkeys: Map[HITTypeID,BatchKey] = hittypes_by_batchkey.map { case (key, hittype) => hittype.id -> key}

    val inserts: List[(HITType, Int)] = hittypes_by_batchkey.values.flatMap { ht =>
      if (existing_batches.contains(ht.id)) {
        None
      } else {
        val batchKey = batchkeys(ht.id)
        val (groupID, _, _) = batchKey
        Some(ht, batch_no(groupID)(batchKey))
      }
    }.toList

    // do HITType inserts
    dbHITType ++= HITType2HITTypeTuples(batchkeys, inserts)

    // do qualification inserts
    val q_inserts = HITType2QualificationTuples(inserts)

    try {
      dbQualReq ++= q_inserts
    } catch {
      case t: Throwable =>
        println(s"hittypes_by_batchkey:\n${hittypes_by_batchkey.mkString("\n")}")
        println(s"batch_no map:\n${batch_no.mkString("\n")}")
        println(s"HITType inserts:\n${inserts.mkString("\n")}")
        println(s"QualificationRequirement inserts:\n${q_inserts.mkString("\n")}")
        throw t
    }
  }

  private def HITType2QualificationTuples(inserts: List[(HITType,Int)]) : List[(Int, String, Int, Comparator, Boolean, Boolean, String)] = {
    implicit val comparatorMapper = DBQualificationRequirement.comparatorMapper
    inserts.map { case (hittype,batch_no) =>
      val d: QualificationRequirement = hittype.disqualification
      // something must be present in the ID field, but since it doesn't
      // matter what it is, we just use 1; the database subs in the
      // appropriate autoincremented ID.
      (1, d.getQualificationTypeId, d.getIntegerValues.get(0), d.getComparator, d.getRequiredToPreview.booleanValue(), true, hittype.id) //TODO: ugh
    }
  }

  private def HITType2HITTypeTuples(batchkeys: Map[HITTypeID,BatchKey], inserts: List[(HITType,Int)]) : List[(HITTypeID, GroupID, BigDecimal, Int, Int)] = {
    inserts.map { case (hittype, batch_no) =>
      (hittype.id, hittype.group_id, batchkeys(hittype.id)._2, batchkeys(hittype.id)._3, batch_no)
    }
  }

  private def allBatchNumbers = {
    dbHITType.map { row => (row.groupId, row.batchNo) }
  }

  private def allHITTypes = {
    implicit val comparatorMapper = DBQualificationRequirement.comparatorMapper

    (dbHITType leftJoin dbQualReq on (_.id === _.HITTypeId)).map {
      case(h, q) => (
        h.id,
        h.groupId,
        h.cost,
        h.timeoutInS,
        h.batchNo,
        q.qualificationTypeId,
        q.comparator,
        q.integerValue,
        q.requiredToPreview,
        q.isDisqualification)
    }
  }

  private def allHITs = {
    dbHIT
  }

  private def allAssignments = {
    dbAssignment
  }

  private def tuple2Assignment(tup: (String, String, String, AssignmentStatus, Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], String, Option[String], UUID)) : Assignment = {
    val (assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime, acceptTime, submitTime, approvalTime, rejectionTime, deadline, answer, requesterFeedback, taskId) = tup
    //new Assignment(null, assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime.orNull, acceptTime.orNull, submitTime.orNull, approvalTime.orNull, rejectionTime.orNull, deadline.orNull, answer, requesterFeedback.orNull)
    new Assignment()
      .withAssignmentId(assignmentId)
      .withWorkerId(workerId)
      .withHITId(hit_id)
      .withAssignmentStatus(assignmentStatus)
      .withAutoApprovalTime(autoApprovalTime.orNull.getTime)
      .withAcceptTime(acceptTime.orNull)
      .withSubmitTime(submitTime.orNull)
      .withApprovalTime(approvalTime.orNull)
      .withRejectionTime(rejectionTime.orNull)
      .withDeadline(deadline.orNull)
      .withAnswer(answer)
      .withRequesterFeedback(requesterFeedback.orNull)
  }

  private def taskAssignmentMap(implicit session: DBSession) : Map[UUID,Assignment] = {
    dbAssignment.list.map(row => row._13 -> tuple2Assignment(row)).toMap
  }

  private def getHITStateMap(htid_map: Map[String, HITType], backend: AmazonMTurk)(implicit session: DBSession) : Map[String,HITState] = {
    // restore all HIT IDs from database
    val hit_ids = allHITs.list.map { case(hit_id, hit_type_id, is_cancelled) => hit_id -> (hit_type_id,is_cancelled) }.toMap

    // TODO fix:
    // because the database only stores the HIT ID and HITType ID, we need
    // to query MTurk to get the rest of the information;
    // this means that this call will fail in testing if the
    // MTAdapter object is completely disposed and constructed again
    // since mock HITs do not persist between runs.
    val hits = hit_ids.keys.map { hit_id => backend.getHIT(new GetHITRequest().withHITId(hit_id)) }

    // restore saved Task-Assignment pairings
    val all_ta_map = taskAssignmentMap

    // group Task IDs by HIT
    val task_ids_by_hitid = dbTaskHIT
      .list
      .groupBy { case (hit_id: String, _) => hit_id }
      .map { case (hit_id: String, tasks: List[(String,UUID)]) =>
        hit_id -> tasks.map { case (_, task_id: UUID) => task_id }
      }

    // return a map from HIT to HITState
    hits.map { hit =>
      // get all the tasks for this HIT
      val task_ids = task_ids_by_hitid(hit.getHITId)

      // make a task-assignment map for this HIT;
      // when a task_id has no entry in the map, insert value None
      val taskAssignmentMapFiltered : Map[UUID,Option[Assignment]] = task_ids.map { task_id =>
        if (all_ta_map.contains(task_id)) {
          task_id -> Some(all_ta_map(task_id))
        } else {
          task_id -> None
        }
      }.toMap

      // construct HITState
      val hit_id = hit.getHITId
      val hit_type_id = hit_ids(hit_id)._1
      val hit_type = htid_map(hit_type_id)
      val cancelled = hit_ids(hit_id)._2
      hit_id -> HITState(hit, taskAssignmentMapFiltered, hit_type, cancelled)
    }.toMap
  }

  private def getHITTypesByHITTypeId(m: Map[Key.BatchKey, HITType]) : Map[String, HITType] = {
    m.values.map { ht => ht.id -> ht }.toMap
  }

  private def getHITTypeMap(implicit session: DBSession) : Map[Key.BatchKey, HITType] = {
    val grps = allHITTypes.list.groupBy {
      case (ht_id, grp_id, cost, timeout, batch_no, _, _, _, _, _) =>
        (ht_id, grp_id, cost, timeout, batch_no)
    }

    grps.map { case (group_key, qualdata) =>
      val (ht_id, grp_id, cost, timeout, batch_no) = group_key

      // TODO: we no longer support "normal qualifications"; simply code and remove
      val disquals = qualdata.flatMap { case (_, _, _, _, _, q_id, comp, iv, reqd, is_disq) =>
        if (is_disq) {
          // a disqualification
          Some(new QualificationRequirement (q_id, comp, iv, null, reqd))
        } else {
          None
        }
      }

      assert(disquals.nonEmpty)

      Key.BatchKey(grp_id, cost, timeout) -> HITType(ht_id, disquals.head, grp_id)
    }
  }

  private def getHITIDMap(implicit session: DBSession) : Map[Key.HITKey, Key.HITID] = {
    val query = dbQuestion join
      dbTask on (_.id === _.question_id) join
      dbTaskHIT on (_._2.task_id === _.taskId) join
      dbHIT on (_._2.HITId === _.HITId) join
      dbHITType on (_._2.HITTypeId === _.id)
    query.map { r => ((r._2.groupId, r._1._1._1._2.cost, r._1._1._1._2.worker_timeout_in_s), r._1._1._1._1.memo_hash) -> r._1._2.HITId }.list.toMap
  }

  private def createQualificationFromType(qualtype: AmazonMTurk, batch_no: Int) : QualificationRequirement = { //TODO: why is qualtype AmazonMTurk?
    new QualificationRequirement().withQualificationTypeId(
      qualtype.getQualificationType(
        new GetQualificationTypeRequest()
      ).getQualificationType().getQualificationTypeId)
      .withComparator(Comparator.EqualTo)
      .withIntegerValues(batch_no)
      .withActionsGuarded("Accept") //TODO: check if correct
    //, Comparator.EqualTo, batch_no, null, false)
    //getQualificationTypeId, Comparator.EqualTo, batch_no, null, false)
  }

  private def getQualRecFromMTurk(qual_id: String, batch_no: Int, backend: AmazonMTurk) : QualificationRequirement = {
    val qual_type = backend.getQualificationType(new GetQualificationTypeRequest().withQualificationTypeId(qual_id))
    createQualificationFromType(qual_type, batch_no)
  }

  private def getWorkerWhitelist(implicit session: DBSession) : Map[(String,String),String] = {
    dbWorker.list.map{ case (workerId, groupId, hittypeid) => (workerId, groupId) -> hittypeid }.toMap
  }

  private def getQualifications(implicit session: DBSession) : Map[Key.QualificationID, Key.HITTypeID] = {
    dbQualReq.map { r => (r.qualificationTypeId, r.HITTypeId) }.list.toMap
  }

  private def getBatchNos(implicit session: DBSession) : Map[GroupID, Map[BatchKey, Int]] = {
    val q = dbHITType.map { r =>
      (r.groupId, r.cost, r.timeoutInS, r.batchNo)
    }.list
    q.foldLeft(Map[GroupID, Map[BatchKey, Int]]()) { case (acc, (group_id, cost, timeout, batch_no)) =>
      val batchKey = BatchKey(group_id, cost, timeout)
      if (!acc.contains(group_id)) {
        acc + (group_id -> Map(batchKey -> batch_no))
      } else {
        val submap = acc(group_id) + (batchKey -> batch_no)
        acc + (group_id -> submap)
      }
    }
  }
}
