package edu.umass.cs.automan.adapters.mturk.logging

import java.util.{UUID, Calendar}

import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.connectionpool.{MTState, HITState, HITType, Pool}
import edu.umass.cs.automan.adapters.mturk.logging.tables.{DBAssignment, DBQualificationRequirement}
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key._
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.Task
import scala.slick.lifted.TableQuery
import scala.slick.driver.SQLiteDriver.simple._

class MTMemo(log_config: LogConfig.Value) extends Memo(log_config) {
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

  // TODO fix: save_mt_state is never called!!!
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

  def assignmentFromDBAssnRow(row: (String, String, String, AssignmentStatus, Calendar, Calendar, Calendar, Calendar, Calendar, Calendar, String, String, UUID)) : Assignment = {
    val (assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime, acceptTime, submitTime, approvalTime, rejectionTime, deadline, answer, requesterFeedback, taskId) = row
    new Assignment(
      null,
      assignmentId,
      workerId,
      hit_id,
      assignmentStatus,
      autoApprovalTime,
      acceptTime,
      submitTime,
      approvalTime,
      rejectionTime,
      deadline,
      answer,
      requesterFeedback
    )
  }

  def updateHITs(hit_states: Map[HITID,HITState], hit_ids: Map[HITKey,HITID])(implicit session: DBSession) : Unit = {
    implicit val statusMapper = DBAssignment.statusMapper

    val existing_hits = allHITs.list.map { case (hit_id, hit_type_id, is_cancelled) => hit_id -> (hit_id, hit_type_id, is_cancelled) }.toMap
    val existing_assignments = allAssignments.list.map { assn => assn._1 -> assn }.toMap

    // HITs
    val (hit_inserts, hit_updates) = hit_states.values.map { hitstate =>
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

    // Assignments
    val (assignment_inserts, assignment_updates) = hit_states.values.map { hitstate =>
      hitstate.t_a_map.flatMap { case (task_id, assignment_opt) =>
        assignment_opt match {
          case Some(assignment) =>
            if (existing_assignments.contains(assignment.getAssignmentId)) {
              val existing_assn = assignmentFromDBAssnRow(existing_assignments(assignment.getAssignmentId))
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
    }.flatten.foldLeft(List.empty[(Assignment,UUID)],List.empty[(Assignment,UUID)]) { case (acc,action) =>
      action match {
        case Insert(data) => (data :: acc._1, acc._2)
        case Update(data) => (acc._1, data :: acc._2)
        case Skip(data) => acc
      }
    }

    // HIT inserts
    dbHIT ++= HITState2HITTuples(hit_inserts.map { hitid => hit_states(hitid)})

    // HIT updates
    hit_updates.foreach { hit_id => dbHIT.filter(_.HITId === hit_id).map(_.isCancelled).update(hit_states(hit_id).isCancelled)}

    // TaskHIT inserts (no updates needed)
    dbTaskHIT ++=HITState2TaskHITTuples(hit_inserts.map { hitid => hit_states(hitid)})

    // Assignment inserts
    dbAssignment ++= Assignment2AssignmentTuple(assignment_inserts)

    // Assignment updates
    assignment_updates.foreach { case (a, task_id) =>
      dbAssignment
        .filter(_.assignmentId === a.getAssignmentId)
        .map{ r => (r.assignmentStatus, r.autoApprovalTime, r.acceptTime, r.submitTime, r.approvalTime, r.rejectionTime, r.deadline, r.requesterFeedback) }
        .update(a.getAssignmentStatus, a.getAutoApprovalTime, a.getAcceptTime, a.getSubmitTime, a.getApprovalTime, a.getRejectionTime, a.getDeadline, a.getRequesterFeedback)
    }
  }

  private def Assignment2AssignmentTuple(pairs: List[(Assignment,UUID)]) : List[(String, String, String, AssignmentStatus, Calendar, Calendar, Calendar, Calendar, Calendar, Calendar, String, String, UUID)] = {
    pairs.map { case (assignment, task_id) =>
      (
        assignment.getAssignmentId,
        assignment.getWorkerId,
        assignment.getHITId,
        assignment.getAssignmentStatus,
        assignment.getAutoApprovalTime,
        assignment.getAcceptTime,
        assignment.getSubmitTime,
        assignment.getApprovalTime,
        assignment.getRejectionTime,
        assignment.getDeadline,
        assignment.getAnswer,
        assignment.getRequesterFeedback,
        task_id
      )
    }
  }

  private def HITState2TaskHITTuples(hitstates: List[HITState]) : List[(String, UUID)] = {
    hitstates.map { hitstate => hitstate.t_a_map.map { case (task_id,_) => (hitstate.HITId, task_id) } }.flatten
  }

  private def HITState2HITTuples(hitstates: List[HITState]) : List[(String, String, Boolean)] = {
    hitstates.map { hitstate => (hitstate.HITId, hitstate.hittype.id, hitstate.isCancelled) }
  }

  // HITTypes and Qualifications never need updating; they are insert-only
  private def updateHITTypes(hts: Map[BatchKey,HITType], batch_no: Map[GroupID, Int])(implicit session: DBSession) : Unit = {
    val existing_batches: Map[HITTypeID, Int] = allHITTypes.map { r => (r._1, r._5) }.list.toMap
    val batchkeys: Map[HITTypeID,BatchKey] = hts.map { case (key, hittype) => hittype.id -> key}.toMap
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
    dbQualReq ++= HITType2QualificationTuples(inserts)
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

  private def tuple2Assignment(tup: (String, String, String, AssignmentStatus, Calendar, Calendar, Calendar, Calendar, Calendar, Calendar, String, String, UUID)) : Assignment = {
    val (assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime, acceptTime, submitTime, approvalTime, rejectionTime, deadline, answer, requesterFeedback, taskId) = tup
    new Assignment(null, assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime, acceptTime, submitTime, approvalTime, rejectionTime, deadline, answer, requesterFeedback)
  }

  private def taskAssignmentMap(implicit session: DBSession) : Map[UUID,Option[Assignment]] = {
    dbAssignment.list.map(row => row._13 -> Some(tuple2Assignment(row))).toMap
  }

  private def getHITStateMap(htid_map: Map[String, HITType], backend: RequesterService)(implicit session: DBSession) : Map[String,HITState] = {
    val hit_ids = allHITs.list.map { case(hit_id, hit_type_id, is_cancelled) => hit_id -> (hit_type_id,is_cancelled) }.toMap
    val hits = hit_ids.keys.map { hit_id => backend.getHIT(hit_id) }
    hits.map { hit =>
      val hit_id = hit.getHITId
      val hit_type_id = hit_ids(hit_id)._1
      val hit_type = htid_map(hit_type_id)
      val cancelled = hit_ids(hit_id)._2
      hit_id -> HITState(hit, taskAssignmentMap, hit_type, cancelled)
    }.toMap
  }

  private def getHITTypesByHITTypeId(m: Map[Key.BatchKey, HITType]) : Map[String, HITType] = {
    m.values.map { ht => ht.id -> ht }.toMap
  }

  private def getHITTypeMap(implicit session: DBSession) : Map[Key.BatchKey, HITType] = {
    val grps = allHITTypes.list.groupBy {
      case (ht_id, grp_id, cost, timeout, batch_no, qual_id, comp, intval, reqd, is_disq) =>
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
    }.toMap
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

  def restore_mt_state(pool: Pool, backend: RequesterService) : Unit = {
    db_opt match {
      case Some(db) => db withSession { implicit s =>
        val hit_types: Map[Key.BatchKey, HITType] = getHITTypeMap
        val id_hittype: Map[String, HITType] = getHITTypesByHITTypeId(hit_types)
        val hit_states: Map[String, HITState] = getHITStateMap(id_hittype, backend)
        val hit_ids: Map[Key.HITKey, Key.HITID] = getHITIDMap

        pool.restoreState(
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
      case None => ()
    }
  }

  /**
   * This call deletes all records stored in all of the Memo
   * database's tables.
   */
  override def wipeDatabase(): Unit = {
    super.wipeDatabase()

    db_opt match {
      case Some(db) =>
        if (database_exists()) {
          db.withSession { implicit session =>
            dbAssignment.delete
            dbHIT.delete
            dbHITType.delete
            dbQualReq.delete
            dbTaskHIT.delete
            dbWorker.delete
          }
        }
      case None => ()
    }
  }
}
