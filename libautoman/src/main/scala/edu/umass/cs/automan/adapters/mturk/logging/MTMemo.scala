package edu.umass.cs.automan.adapters.mturk.logging

import java.util.{UUID, Calendar}
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.connectionpool.{MTState, HITState, HITType, Pool}
import edu.umass.cs.automan.adapters.mturk.logging.tables.{DBAssignment, DBQualificationRequirement}
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key._
import edu.umass.cs.automan.core.logging._
import scala.slick.driver.H2Driver.simple._

class MTMemo(log_config: LogConfig.Value, database_path: String) extends Memo(log_config, database_path) {
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
      new Assignment(
        null,
        this.assignmentId,
        this.workerId,
        this.HITId,
        this.assignmentStatus,
        this.autoApprovalTime.orNull,
        this.acceptTime.orNull,
        this.submitTime.orNull,
        this.approvalTime.orNull,
        this.rejectionTime.orNull,
        this.deadline.orNull,
        this.answer,
        this.requesterFeedback.orNull
      )
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

  // to prevent data races, access to this method
  // should be serialized by a lock
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

  def restore_mt_state(backend: RequesterService) : Option[MTState] = {
    db_opt match {
      case Some(db) => db withSession { implicit s =>
        // debug
        val hittypes = dbHITType.list
        val quals = dbQualReq.list

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

  def updateWhitelist(ww: Map[(WorkerID,GroupID),HITTypeID])(implicit session: DBSession) : Unit = {
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

  def getHITInsertsAndUpdates(existing_hits: Map[String, (String, String, Boolean)], hit_states: Map[HITID,HITState])
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

  def getAssignmentInsertsAndUpdates(existing_assignments: Map[String, Assn], hit_states: Map[HITID,HITState])
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

  def updateHITs(hit_states: Map[HITID,HITState], hit_ids: Map[HITKey,HITID])(implicit session: DBSession) : Unit = {
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

    // lists should only contain distinct elements
    assert(hit_inserts.distinct.length == hit_inserts.length)
    assert(hit_updates.distinct.length == hit_updates.length)

    // Assignments
    val (assignment_inserts, assignment_updates) = getAssignmentInsertsAndUpdates(existing_assignments, hit_states)

    // lists should only contain distinct elements
    assert(assignment_inserts.map(_._1.getAssignmentId).distinct.length == assignment_inserts.length)
    assert(assignment_updates.map(_._1.getAssignmentId).distinct.length == assignment_updates.length)

    // HIT inserts
    dbHIT ++= HITState2HITTuples(hit_inserts.map { hitid => hit_states(hitid)} )

    // mark cancelled HITs as cancelled in the database
    hit_updates.foreach { hit_id => dbHIT.filter(_.HITId === hit_id).map(_.isCancelled).update(hit_states(hit_id).isCancelled)}

    // TaskHIT inserts (no updates needed)
    dbTaskHIT ++= HITState2TaskHITTuples(hit_inserts.map { hitid => hit_states(hitid)})

    // Assignment inserts
    val a_inserts = Assignment2AssignmentTuple(assignment_inserts)
    dbAssignment ++= a_inserts

    // Assignment updates
    // a.getAssignmentStatus, a.getAutoApprovalTime, a.getAcceptTime, a.getSubmitTime, a.getApprovalTime, a.getRejectionTime, a.getDeadline, a.getRequesterFeedback
    assignment_updates.foreach { case (a, task_id) =>
      dbAssignment
        .filter(_.assignmentId === a.getAssignmentId)
        .map{ r => (r.assignmentStatus, r.autoApprovalTime, r.acceptTime, r.submitTime, r.approvalTime, r.rejectionTime, r.deadline, r.requesterFeedback) }
        .update(a.getAssignmentStatus, Option(a.getAutoApprovalTime), Option(a.getAcceptTime), Option(a.getSubmitTime), Option(a.getApprovalTime), Option(a.getRejectionTime), Option(a.getDeadline), Option(a.getRequesterFeedback))
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
  private def updateHITTypes(hts: Map[BatchKey,HITType], batch_no: Map[GroupID, Int])(implicit session: DBSession) : Unit = {
    val existing_batches: Map[HITTypeID, Int] = allHITTypes.map { r => (r._1, r._5) }.list.toMap
    val batchkeys: Map[HITTypeID,BatchKey] = hts.map { case (key, hittype) => hittype.id -> key}
    val inserts: List[(HITType, Int)] = hts.values.flatMap { ht =>
      if (existing_batches.contains(ht.id)) {
        None
      } else {
        Some((ht,batch_no(ht.group_id)))
      }
    }.toList

    // do HITType inserts
    dbHITType ++= HITType2HITTypeTuples(batchkeys, inserts)

    // do qualification inserts
    val q_inserts = HITType2QualificationTuples(inserts)
    dbQualReq ++= q_inserts
  }

  private def HITType2QualificationTuples(inserts: List[(HITType,Int)]) : List[(String, Int, Comparator, Boolean, Boolean, String)] = {
    implicit val comparatorMapper = DBQualificationRequirement.comparatorMapper
    inserts.map { case (hittype,batch_no) =>
      val d = hittype.disqualification
      val qual = (d.getQualificationTypeId, d.getIntegerValue.toInt, d.getComparator, d.getRequiredToPreview.booleanValue(), true, hittype.id)
      val quals = hittype.quals.map { qr =>
        (qr.getQualificationTypeId, qr.getIntegerValue.toInt, qr.getComparator, qr.getRequiredToPreview.booleanValue(), false, hittype.id)
      }
      qual :: quals
    }.flatten
  }

  private def HITType2HITTypeTuples(batchkeys: Map[HITTypeID,BatchKey], inserts: List[(HITType,Int)]) : List[(HITTypeID, GroupID, BigDecimal, Int, Int)] = {
    inserts.map { case (hittype, batch_no) =>
      (hittype.id, hittype.group_id, batchkeys(hittype.id)._2, batchkeys(hittype.id)._3, batch_no)
    }
  }

  private def allBatchNumbers = {
    dbHITType.map { row => (row.groupId, row.maxBatchNo) }
  }

  private def allHITTypes = {
    implicit val comparatorMapper = DBQualificationRequirement.comparatorMapper

    (dbHITType leftJoin dbQualReq on (_.id === _.HITTypeId)).map {
      case(h, q) => (
        h.id,
        h.groupId,
        h.cost,
        h.timeoutInS,
        h.maxBatchNo,
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
    new Assignment(null, assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime.orNull, acceptTime.orNull, submitTime.orNull, approvalTime.orNull, rejectionTime.orNull, deadline.orNull, answer, requesterFeedback.orNull)
  }

  private def taskAssignmentMap(implicit session: DBSession) : Map[UUID,Assignment] = {
    dbAssignment.list.map(row => row._13 -> tuple2Assignment(row)).toMap
  }

  private def getHITStateMap(htid_map: Map[String, HITType], backend: RequesterService)(implicit session: DBSession) : Map[String,HITState] = {
    val hit_ids = allHITs.list.map { case(hit_id, hit_type_id, is_cancelled) => hit_id -> (hit_type_id,is_cancelled) }.toMap
    val hits = hit_ids.keys.map { hit_id => backend.getHIT(hit_id) }

    val all_ta_map = taskAssignmentMap

    val th = dbTaskHIT.list

    val task_ids_by_hitid = dbTaskHIT
      .list
      .groupBy { case (hit_id: String, _) => hit_id }
      .map { case (hit_id: String, tasks: List[(String,UUID)]) =>
        hit_id -> tasks.map { case (_, task_id: UUID) => task_id }
      }

    // we want to construct a task-assignment map specifically for this HITState
    hits.map { hit =>
      val task_ids = task_ids_by_hitid(hit.getHITId)

      // when a task_id has no entry in the map, insert value None
      val taskAssignmentMapFiltered : Map[UUID,Option[Assignment]] = task_ids.map { task_id =>
        if (all_ta_map.contains(task_id)) {
          task_id -> Some(all_ta_map(task_id))
        } else {
          task_id -> None
        }
      }.toMap

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

      val quals = qualdata.map { case (_, _, _, _, _, q_id, comp, iv, reqd, is_disq) =>
        if (is_disq) {
          // a disqualification
          Left(new QualificationRequirement(q_id, comp, iv, null, reqd))
        } else {
          // a normal qualification
          Right(new QualificationRequirement(q_id, comp, iv, null, reqd))
        }

      }
      val normal_quals = quals.flatMap { case Right(q) => Some(q); case Left(q) => None }
      val disqual = quals.flatMap { case Right(q) => None; case Left(q) => Some(q) }.head

      Key.BatchKey(grp_id, cost, timeout) -> HITType(ht_id, normal_quals, disqual, grp_id)
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

  private def createQualificationFromType(qualtype: QualificationType, batch_no: Int) : QualificationRequirement = {
    new QualificationRequirement(qualtype.getQualificationTypeId, Comparator.EqualTo, batch_no, null, false)
  }

  private def getQualRecFromMTurk(qual_id: String, batch_no: Int, backend: RequesterService) : QualificationRequirement = {
    val qual_type = backend.getQualificationType(qual_id)
    createQualificationFromType(qual_type, batch_no)
  }

  private def getWorkerWhitelist(implicit session: DBSession) : Map[(String,String),String] = {
    dbWorker.list.map{ case (workerId, groupId, hittypeid) => (workerId, groupId) -> hittypeid }.toMap
  }

  private def getQualifications(implicit session: DBSession) : Map[Key.QualificationID, Key.HITTypeID] = {
    dbQualReq.map { r => (r.qualificationTypeId, r.HITTypeId) }.list.toMap
  }

  private def getBatchNos(implicit session: DBSession) : Map[GroupID, Int] = {
    dbHITType.map { r => (r.groupId, r.maxBatchNo)}.list.toMap
  }
}
