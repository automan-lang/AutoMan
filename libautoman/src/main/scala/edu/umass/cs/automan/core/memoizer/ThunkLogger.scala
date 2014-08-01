package edu.umass.cs.automan.core.memoizer

import net.java.ao._
import edu.umass.cs.automan.core.scheduler._

class ThunkLogger(DBConnString: String, user: String, password: String) {
  // init
  private val _manager = new EntityManager(DBConnString, user, password)
  _manager.migrate(classOf[ThunkMemo])

  def writeThunk(t: Thunk[_], state: SchedulerState.Value, worker_id: String) = synchronized {
    val tl = _manager.create[ThunkMemo,java.lang.Integer](classOf[ThunkMemo])
    tl.setCompletionTime(new java.util.Date())
    tl.setCostInCents((t.cost * 100).toInt)
    tl.setComputationId(t.computation_id.toString)
    tl.setQuestionId(t.question.id.toString)
    tl.setCreationTime(t.created_at)
    tl.setExpirationDate(t.expires_at)
    val ms = state match {
      case SchedulerState.ACCEPTED => MemoState.ACCEPTED
      case SchedulerState.REJECTED => MemoState.REJECTED
      case SchedulerState.TIMEOUT => MemoState.TIMEOUT
      case SchedulerState.CANCELLED => MemoState.CANCELLED
    }
    if (worker_id != null) tl.setWorkerId(worker_id)
    tl.setState(ms)
    tl.save()
  }
}