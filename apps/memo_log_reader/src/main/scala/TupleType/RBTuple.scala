package TupleType

import com.amazonaws.mturk.requester.HIT
import edu.umass.cs.automan.adapters.MTurk.memoizer.MTurkAnswerCustomInfo
import edu.umass.cs.automan.core.memoizer.RadioButtonAnswerMemo

import scala.util.matching.Regex

object RBTuple {
  private val headers = List(
    "id",                // 0
    "memo_hash",         // 1
    "worker_id",         // 2
    "paid_status",       // 3
    "answer_value",      // 4
    "assignment_id",     // 5
    "hit_id",            // 6
    "image_name",        // 7
    "reward"             // 8
  )
  def header = headers.mkString(",")
  def fromRadioButtonAnswerMemo(answer: RadioButtonAnswerMemo) : RBTuple = {
    new RBTuple(
      answer.getID,
      answer.getMemoHash,
      answer.getWorkerId,
      answer.getPaidStatus,
      answer.getAnswerValue,
      answer.getCustomInfo
    )
  }
  def writeHeaderToLog(csv: log.CSV) {
    csv.addRow(headers:_*)
  }
}

class RBTuple(id: Int, memo_hash: String, worker_id: String, paid_status: Boolean, answer_value: String, custom_info: String) {
  // parse custom info field
  private val _ci = new MTurkAnswerCustomInfo()
  _ci.parse(custom_info)
  val assignment_id = _ci.assignment_id
  val hit_id = _ci.hit_id

  var hit: HIT = null

  private def fields = List(
    id.toString,              // 0
    memo_hash,                // 1
    worker_id,                // 2
    paid_status.toString,     // 3
    answer_value,             // 4
    assignment_id,            // 5
    hit_id,                   // 6
    getImageURL(),            // 7
    getReward().toString      // 8
  )
  def getImageURL() : String = {
    if (hit == null) {
      ""
    } else {
      val q: String = hit.getQuestion
      val r: Regex = "<DataURL>(.+)</DataURL>".r
      r.findFirstMatchIn(q) match {
        case Some(m) => m.group(1)
        case None => ""
      }
    }
  }
  def getReward() : BigDecimal = {
    if (hit == null) {
      BigDecimal(0)
    } else {
      hit.getReward.getAmount
    }
  }
  def setHIT(h: HIT) { hit = h }
  override def toString = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s", fields:_*)
  def writeLog(csv: log.CSV) { csv.addRow(fields:_*) }
}