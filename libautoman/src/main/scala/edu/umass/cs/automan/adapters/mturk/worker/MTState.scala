package edu.umass.cs.automan.adapters.mturk.worker

//import com.amazonaws.mturk.requester.Assignment
//import software.amazon.awssdk.services.mturk.model.Assignment
import com.amazonaws.services.mturk.model.Assignment
import edu.umass.cs.automan.adapters.mturk.util.Key
import edu.umass.cs.automan.adapters.mturk.util.Key._
import edu.umass.cs.automan.core.logging.{LogType, LogLevelDebug, DebugLog}
import edu.umass.cs.automan.core.scheduler.Task

case class MTState(hit_types: Map[BatchKey,HITType],
                   hit_states: Map[HITID,HITState],
                   hit_ids: Map[HITKey,HITID],
                   worker_whitelist: Map[(WorkerID,GroupID),HITTypeID],
                   disqualifications: Map[QualificationID,HITTypeID],
                   batch_no: Map[GroupID, Map[BatchKey, Int]]) {
  def this() =
    this(
      Map[BatchKey,HITType](),
      Map[HITID,HITState](),
      Map[HITKey,HITID](),
      Map[(WorkerID,GroupID),HITTypeID](),
      Map[QualificationID,HITTypeID](),
      Map[GroupID,Map[BatchKey,Int]]()
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
  def addDisqualifications(qualificationID: QualificationID, hittypeid: HITTypeID) : MTState = {
    MTState(hit_types, hit_states, hit_ids, worker_whitelist, disqualifications + (qualificationID -> hittypeid), batch_no)
  }
  def deleteDisqualifications() : MTState = {
    MTState(hit_types, hit_states, hit_ids, worker_whitelist, Map.empty, batch_no)
  }
  def updateBatchNo(batchKey: BatchKey) : MTState = {
    val groupID = batchKey._1

    if (!batch_no.contains(groupID)) {
      // first run
      DebugLog(s"First run for batch ${batchKey}; initializing batch_nor to 1.", LogLevelDebug(), LogType.ADAPTER, null)
      val batch_map = Map(batchKey -> 1)
      MTState(hit_types, hit_states, hit_ids, worker_whitelist, disqualifications, batch_no + (groupID -> batch_map))
    } else {
      // not first run; is there an applicable batch number?
      val batch_map = batch_no(groupID)
      if (batch_map.contains(batchKey)) {
        // yes, we already have a batch number
        val batchNo = batch_map(batchKey)
        DebugLog(s"Found batch_no ${batchNo} for ${batchKey}; reusing.", LogLevelDebug(), LogType.ADAPTER, null)
        // don't change anything
        this
      } else {
        // no, there's no batch number; find the largest existing batch number and increment it
        val batchNo = batch_map.values.max + 1
        DebugLog(s"New batch ${batchKey} for group_id ${groupID}; incrementing batch_no to ${batchNo}.", LogLevelDebug(), LogType.ADAPTER, null)
        val batch_map2 = batch_map + (batchKey -> batchNo)
        MTState(hit_types, hit_states, hit_ids, worker_whitelist, disqualifications, batch_no + (groupID -> batch_map2))
      }
    }
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
    val hit_id = hit_ids(hit_key) // TODO this is throwing
    hit_states(hit_id)
  }
  def getBatchKeyByHITTypeId(hitTypeID: HITTypeID) : Option[BatchKey] = {
    hit_types.filter { case (batch_key, hit_type) => hit_type.id == hitTypeID }.toList match {
      case head :: tail => Some(head._1)
      case Nil => None
    }
  }
  def getBatchNo(batchKey: BatchKey) : Option[Int] = {
    val groupID = batchKey._1
    if (batch_no.contains(groupID) && batch_no(groupID).contains(batchKey)) {
      Some(batch_no(groupID)(batchKey))
    } else {
      None
    }
  }
  def isFirstRun(group_id: GroupID) : Boolean = {
    !hit_types.map{ case ((gid, _, _), _) => gid }.toSet.contains(group_id)
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
  def getHITTypeForWhitelistedWorker(workerID: WorkerID, groupID: GroupID) : HITType = {
    val htid = worker_whitelist(workerID, groupID)
    hit_types.filter { case (_,hittype) => hittype.id == htid}.head._2
  }
  def getHITTypeIDforQualificationTypeID(qualificationID: QualificationID) : HITTypeID = {
    disqualifications(qualificationID)
  }
}
