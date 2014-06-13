package TupleType

import edu.umass.cs.automan.core.memoizer.{ThunkMemo, MemoState}

object ThunkTuple {
  private val headers = List(
    "id",                // 0
    "creation_time",     // 1
    "completion_time",   // 2
    "expiration_date",   // 3
    "cost_in_cents",     // 4
    "worker_id",         // 5
    "computation_id",    // 6
    "state"              // 7
  )
  def fromThunkMemo(thunk_memo: ThunkMemo) : ThunkTuple = {
    new ThunkTuple(
      thunk_memo.getID,
      thunk_memo.getCreationTime,
      thunk_memo.getCompletionTime,
      thunk_memo.getCostInCents,
      thunk_memo.getWorkerId,
      thunk_memo.getQuestionId,
      thunk_memo.getComputationId,
      thunk_memo.getExpirationDate,
      thunk_memo.getState
    )
  }
  def header = headers.mkString(",") + String.format("%n")
  def writeHeaderToLog(csv: log.CSV) {
    csv.addRow(headers:_*)
  }
}

class ThunkTuple(id: Int,
                      creation_time: java.util.Date,
                      completion_time: java.util.Date,
                      cost_in_cents: Int,
                      worker_id: String,
                      question_id: String,
                      computation_id: String,
                      expiration_date: java.util.Date,
                      state: MemoState) {
  private def fields = List(
    id.toString,
    creation_time.toString,
    completion_time.toString,
    expiration_date.toString,
    cost_in_cents.toString,
    worker_id,
    computation_id,
    state.toString
  )
  override def toString = String.format("%s,%s,%s,%s,%s,%s,%s,%s", fields:_*)
  def writeLog(csv: log.CSV) { csv.addRow(fields:_*) }
}