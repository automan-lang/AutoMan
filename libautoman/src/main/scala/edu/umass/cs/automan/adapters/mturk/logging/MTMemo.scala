package edu.umass.cs.automan.adapters.mturk.logging

import java.util.{UUID, Calendar}

import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.connectionpool.{HITState, HITType, Pool}
import edu.umass.cs.automan.adapters.mturk.logging.tables.DBQualificationRequirement
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.core.logging.{LogConfig, Memo}
import scala.slick.lifted.TableQuery
import scala.slick.driver.DerbyDriver.simple._

class MTMemo(log_config: LogConfig.Value) extends Memo(log_config) {
  type DBHITType = (String, String, Int)
  type DBQualificationRequirement = (String, String)

  // TableQuery aliases
  private val dbAssignment = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBAssignment]
  private val dbHIT = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBHIT]
  private val dbHITType = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBHITType]
  private val dbQualReq = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBQualificationRequirement]

  def save_mt_state() : Unit = {
    ???
  }

  private def allBatchNumbers = {
    dbHITType.map { row => (row.groupId, row.maxBatchNo) }
  }

  private def allHITTypes = {
    implicit val comparatorMapper = DBQualificationRequirement.mapper

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

//
  private def tuple2Assignment(tup: (String, String, String, AssignmentStatus, Calendar, Calendar, Calendar, Calendar, Calendar, Calendar, String, String, UUID)) : Assignment = {
    val (assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime, acceptTime, submitTime, approvalTime, rejectionTime, deadline, answer, requesterFeedback, thunkId) = tup
    new Assignment(null, assignmentId, workerId, hit_id, assignmentStatus, autoApprovalTime, acceptTime, submitTime, approvalTime, rejectionTime, deadline, answer, requesterFeedback)
  }

  private def thunkAssignmentMap : Map[UUID,Option[Assignment]] = {
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
      hit_id -> HITState(hit, thunkAssignmentMap, hit_type, cancelled)
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

  private def createQualificationFromType(qualtype: QualificationType, batch_no: Int) : QualificationRequirement = {
    new QualificationRequirement(qualtype.getQualificationTypeId, Comparator.EqualTo, batch_no, null, false)
  }

  private def getQualRecFromMTurk(qual_id: String, batch_no: Int, backend: RequesterService) : QualificationRequirement = {
    val qual_type = backend.getQualificationType(qual_id)
    createQualificationFromType(qual_type, batch_no)
  }

  def restore_mt_state(pool: Pool, backend: RequesterService) : Unit = {
    db_opt match {
      case Some(db) => db withSession { implicit s =>
        val hit_types: Map[(String, BigDecimal, Int), HITType] = getHITTypeMap
        val id_hittype: Map[String, HITType] = getHITTypesByHITTypeId(hit_types)
        val hit_states: Map[String, HITState] = getHITStateMap(id_hittype, backend)

        pool.restoreBatchNumbers(allBatchNumbers.list.toMap)
        pool.restoreHITTypes(hit_types)
        pool.restoreHITStates(hit_states)
        pool.restoreHITIDs(???)
        pool.restoreWhitelist(???)
      }
      case None => ()
    }
  }
}
