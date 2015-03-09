package edu.umass.cs.automan.adapters.mturk.util

import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.scheduler.Thunk

object Key {
  type HITID = String
  type BatchKey = (String,BigDecimal,Int)   // (group_id, cost, timeout); uniquely identifies a batch
  type HITKey = (BatchKey, String)          // (BatchKey, memo_hash); uniquely identifies a HIT
  type QualificationID = String
  type HITTypeID = String
  type WorkerID = String
  type GroupID = String

  protected[mturk] def BatchKey(t: Thunk[_]) : BatchKey =
    BatchKey(t.question.asInstanceOf[MTurkQuestion].group_id, t.cost, t.worker_timeout)
  protected[mturk] def BatchKey(group_id: String, cost: BigDecimal, timeout_in_s: Int) =
    (group_id, cost, timeout_in_s)
  protected[mturk] def HITKeyForBatch(batch_key: BatchKey, t: Thunk[_]) : HITKey =
    (batch_key, t.question.memo_hash)
  protected[mturk] def HITKey(t: Thunk[_]) : HITKey =
    HITKeyForBatch(BatchKey(t), t)
  protected[mturk] def HITIDsForBatch(batch_key: BatchKey, hit_ids: Map[HITKey,HITID]) : List[HITID] =
    hit_ids.flatMap { case ((bkey, _), hit_id) => if (bkey == batch_key) Some(hit_id) else None }.toList
}
