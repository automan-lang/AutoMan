package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.memoizer.Memo
import edu.umass.cs.automan.core.question.Question
import java.util.{UUID, Calendar, Date}
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

class Thunk[T](val memo: Memo,
               val thunk_id: UUID,
               val question: Question[T],
               val timeout_in_s: Int,
               val worker_timeout: Int,
               val cost: BigDecimal,
               val computation_id: UUID,
               val created_at: Date,
               val state: SchedulerState.Value,
               val from_memo: Boolean,
               val worker_id: Option[String],
               val answer: Option[T],
               val completed_at: Option[Date]
              ) {
  def this(memo: Memo,
           thunk_id: UUID,
           question: Question[T],
           timeout_in_s: Int,
           worker_timeout: Int,
           cost: BigDecimal,
           computation_id: UUID,
           created_at: Date,
           state: SchedulerState.Value,
           from_memo: Boolean) {
    this(memo, thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, state, from_memo, None, None, None)
    Utilities.DebugLog("New Thunk " + thunk_id.toString +  "; will expire at: " + expires_at.toString, LogLevel.INFO, LogType.SCHEDULER, computation_id)
  }
  val expires_at : Date = {
    val calendar = Calendar.getInstance()
    calendar.setTime(created_at)
    calendar.add(Calendar.SECOND, timeout_in_s)
    calendar.getTime
  }
  // add to database
  

  def is_timedout: Boolean = {
    expires_at.before(new Date())
  }
  def copy_as_running() = {
    Utilities.DebugLog("Thunk " + thunk_id.toString +  " changed to RUNNING state; will expire at: " + expires_at.toString, LogLevel.INFO, LogType.SCHEDULER, computation_id)
    new Thunk[T](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.RUNNING, from_memo, worker_id, answer, None)
  }
  def copy_with_answer(ans: T, wrk_id: String) = {
    Utilities.DebugLog("Thunk " + thunk_id.toString +  " changed to RETRIEVED state with answer \"" + ans.toString + "\"; will expire at: " + expires_at.toString, LogLevel.INFO, LogType.SCHEDULER, computation_id)
    new Thunk[T](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.RETRIEVED, from_memo, Some(wrk_id), Some(ans), Some(new Date()))
  }
  def copy_as_timeout() = {
    Utilities.DebugLog("Thunk " + thunk_id.toString +  " changed to TIMEOUT state; expired at: " + expires_at.toString, LogLevel.INFO, LogType.SCHEDULER, computation_id)
    new Thunk[T](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.TIMEOUT, from_memo, worker_id, answer, Some(new Date()))
  }
  def copy_as_processed() = {
    assert(completed_at != None)
    Utilities.DebugLog("Thunk " + thunk_id.toString +  " changed to PROCESSED state.", LogLevel.INFO, LogType.SCHEDULER, computation_id)
    new Thunk[T](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.PROCESSED, from_memo, worker_id, answer, None)
  }
  def copy_as_cancelled() = {
    Utilities.DebugLog("Thunk " + thunk_id.toString +  " changed to CANCELLED state.", LogLevel.INFO, LogType.SCHEDULER, computation_id)
    new Thunk[T](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.CANCELLED, from_memo, worker_id, answer, Some(new Date()))
  }
  def copy_as_accepted() = {
    assert(completed_at != None)
    Utilities.DebugLog("Thunk " + thunk_id.toString +  " changed to ACCEPTED state.", LogLevel.INFO, LogType.SCHEDULER, computation_id)
    new Thunk[T](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.ACCEPTED, from_memo, worker_id, answer, None)
  }
  def copy_as_rejected() = {
    assert(completed_at != None)
    Utilities.DebugLog("Thunk " + thunk_id.toString +  " changed to REJECTED state.", LogLevel.INFO, LogType.SCHEDULER, computation_id)
    new Thunk[T](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.REJECTED, from_memo, worker_id, answer, None)
  }
  override def toString = {
    val has_answer = answer match { case Some(_) => "yes"; case None => "no" }
    val wid = worker_id match { case Some(wid) => wid; case None => "n/a" }
    "Thunk(state: " + state + ", has answer: " + has_answer + ", completed by worker_id: " + wid + ")"
  }
}
