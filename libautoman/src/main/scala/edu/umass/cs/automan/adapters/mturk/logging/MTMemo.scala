package edu.umass.cs.automan.adapters.mturk.logging

import com.amazonaws.mturk.requester.{Comparator, QualificationType, QualificationRequirement}
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.connectionpool.{HITType, Pool}
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.core.logging.{LogConfig, Memo}
import scala.slick.lifted.TableQuery
import scala.slick.driver.DerbyDriver.simple._

class MTMemo(log_config: LogConfig.Value) extends Memo(log_config) {
  type DBHITType = (String, String, Int)
  type DBQualificationRequirement = (String, String)

  // TableQuery aliases
  private val dbHITType = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBHITType]
  private val dbQualReq = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBQualificationRequirement]

  def save_mt_state() : Unit = {
    ???
  }

  private def allBatchNumbers = {
    dbHITType.map { row => (row.groupId, row.maxBatchNo) }
  }

  private def allHITTypes = {
    // TODO JOIN WITH BATCHNUMBERS
    (dbHITType leftJoin dbQualReq on (_.id === _.HITTypeId)).map {
      case(h, q) => (h.id, h.groupId, h.cost, h.timeoutInS, q.qualificationTypeId)
    }
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
        pool.restoreBatchNumbers(allBatchNumbers.list.toMap)

        val hit_type_groups = allHITTypes.list.groupBy { case (id, group_id, cost, timeout, _, _) =>
          (id, group_id, cost, timeout)
        }.map { case ((id, group_id, cost, timeout), values: List[(String, String, BigDecimal, Int, String)]) =>
          val quals = values.map { case (_, _, _, _, qual_id) => getQualRecFromMTurk(qual_id, backend) }
          val hittype = HITType()
        }

        pool.restoreHITTypes(???)
      }
      case None => ()
    }
  }
}
