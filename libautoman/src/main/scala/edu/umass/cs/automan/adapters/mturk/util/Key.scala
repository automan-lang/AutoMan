package edu.umass.cs.automan.adapters.MTurk.util

import edu.umass.cs.automan.adapters.MTurk.question.MTurkQuestion
import edu.umass.cs.automan.core.scheduler.Task

object Key {
  type HITID = String
  type BatchKey = (String,BigDecimal,Int)   // (group_id, cost, timeout); uniquely identifies a batch
  type HITKey = (BatchKey, String)          // (BatchKey, memo_hash); uniquely identifies a HIT
  type QualificationID = String
  type HITTypeID = String
  type WorkerID = String
  type GroupID = String

  protected[MTurk] def BatchKey(t: Task) : BatchKey =
    BatchKey(t.question.asInstanceOf[MTurkQuestion].group_id, t.cost, t.worker_timeout)
  protected[MTurk] def BatchKey(group_id: String, cost: BigDecimal, timeout_in_s: Int) =
    (group_id, cost, timeout_in_s)
  protected[MTurk] def HITKeyForBatch(batch_key: BatchKey, t: Task) : HITKey =
    (batch_key, t.question.memo_hash)
  protected[MTurk] def HITKey(t: Task) : HITKey =
    HITKeyForBatch(BatchKey(t), t)
  protected[MTurk] def HITIDsForBatch(batch_key: BatchKey, hit_ids: Map[HITKey,HITID]) : List[HITID] =
    hit_ids.flatMap { case ((bkey, _), hit_id) => if (bkey == batch_key) Some(hit_id) else None }.toList
}
