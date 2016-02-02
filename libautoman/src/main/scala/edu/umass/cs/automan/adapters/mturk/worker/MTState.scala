package edu.umass.cs.automan.adapters.mturk.worker

import com.amazonaws.mturk.requester.Assignment
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key._
import edu.umass.cs.automan.core.scheduler.Task

case class MTState(hit_types: Map[BatchKey,HITType],
                   hit_states: Map[HITID,HITState],
                   hit_ids: Map[HITKey,HITID],
                   worker_whitelist: Map[(WorkerID,GroupID),HITTypeID],
                   disqualifications: Map[QualificationID,HITTypeID],
                   batch_no: Map[GroupID, Int]) {
  def this() =
    this(
      Map[BatchKey,HITType](),
      Map[HITID,HITState](),
      Map[HITKey,HITID](),
      Map[(WorkerID,GroupID),HITTypeID](),
      Map[QualificationID,HITTypeID](),
      Map[GroupID,Int]()
    )
  def updateHITTypes(batch_key: BatchKey, hit_type: HITType) : MTState = {
    MTState(hit_types + (batch_key -> hit_type), hit_states, hit_ids, worker_whitelist, disqualifications, batch_no)
  }
  def updateHITStates(hit_id: HITID, hit_state: HITState) : MTState = {
    MTState(hit_types, hit_states + (hit_id -> hit_state), hit_ids, worker_whitelist, disqualifications, batch_no)
  }
  def updateHITStates(pairs: Seq[(HITID,HITState)]) : MTState = {
    MTState(hit_types, hit_states ++ pairs, hit_ids, worker_whitelist, disqualifications, batch_no)
  }
  def updateHITIDs(hit_key: HITKey, hit_id: HITID) : MTState = {
    MTState(hit_types, hit_states, hit_ids + (hit_key -> hit_id), worker_whitelist, disqualifications, batch_no)
  }
  def updateWorkerWhitelist(worker_id: WorkerID, group_id: GroupID, hit_type_id: HITTypeID) : MTState = {
    MTState(hit_types, hit_states, hit_ids, worker_whitelist + ((worker_id,group_id) -> hit_type_id), disqualifications, batch_no)
  }
  def updateDisqualifications(qualificationID: QualificationID, hittypeid: HITTypeID) : MTState = {
    MTState(hit_types, hit_states, hit_ids, worker_whitelist, disqualifications + (qualificationID -> hittypeid), batch_no)
  }
  def updateBatchNo(groupID: GroupID, batchNo: Int) : MTState = {
    MTState(hit_types, hit_states, hit_ids, worker_whitelist, disqualifications, batch_no + (groupID -> batchNo))
  }
  def getAssignmentOption(t: Task) : Option[Assignment] = {
    hit_states(hit_ids(Key.HITKey(t))).getAssignmentOption(t)
  }
  def getHITID(t: Task) : HITID = {
    hit_ids(Key.HITKey(t))
  }
  def getHITState(hitID: HITID) : HITState = {
    hit_states(hitID)
  }
  def getHITState(hit_key: HITKey) : HITState = {
    val hit_id = hit_ids(hit_key)
    hit_states(hit_id)
  }
  def getBatchNo(groupID: GroupID) : Int = {
    batch_no(groupID)
  }
  def isFirstRun(group_id: GroupID) : Boolean = {
    !hit_types.map{ case ((gid, _, _), _) => gid }.toSet.contains(group_id)
  }
  def initOrUpdateBatchNo(group_id: GroupID) : MTState = {
    MTState(
      hit_types,
      hit_states,
      hit_ids,
      worker_whitelist,
      disqualifications,
      batch_no + (if (isFirstRun(group_id)) group_id -> 1 else group_id -> (batch_no(group_id) + 1))
    )
  }
  def getHITType(batch_key: BatchKey) : HITType = {
    assert(hit_types.contains(batch_key),
      "Looking for:\n" + batch_key +
        "\nhit_types contains:\n" + hit_types
    )
    hit_types(batch_key)
  }
  def getHITIDsForBatch(batch_key: BatchKey) : List[HITID] = {
    Key.HITIDsForBatch(batch_key, hit_ids)
  }
  def getWhitelistedHITType(workerID: WorkerID, groupID: GroupID) : HITTypeID = {
    worker_whitelist(workerID, groupID)
  }
  def getHITTypeIDforQualificationTypeID(qualificationID: QualificationID) : HITTypeID = {
    disqualifications(qualificationID)
  }
}
