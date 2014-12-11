package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.question.Question
import java.util.{UUID, Calendar, Date}
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

class Thunk[A <: Answer](val thunk_id: UUID,
                         val question: Question,
                         val timeout_in_s: Int,
                         val worker_timeout: Int,
                         val cost: BigDecimal,
                         val computation_id: UUID,
                         val created_at: Date,
                         val state: SchedulerState.Value,
                         val from_memo: Boolean,
                         val worker_id: Option[String],
                         val answer: Option[A],
                         val completed_at: Option[Date]
                        ) {
  def this(thunk_id: UUID,
       question: Question,
       timeout_in_s: Int,
       worker_timeout: Int,
       cost: BigDecimal,
       computation_id: UUID,
       created_at: Date,
       state: SchedulerState.Value,
       from_memo: Boolean,
       worker_id: Option[String],
       answer: Option[A]) {
    this(thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, state, from_memo, worker_id, answer, None)
  }
  val expires_at : Date = {
    val calendar = Calendar.getInstance()
    calendar.setTime(created_at)
    calendar.add(Calendar.SECOND, timeout_in_s)
    calendar.getTime
  }
  def is_timedout: Boolean = {
    val now = new Date()
    expires_at.before(now)
  }
  def copy_as_running() = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.RUNNING, from_memo, worker_id, answer)
  }
  def copy_with_answer(ans: A, wrk_id: String) = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.RETRIEVED, from_memo, Some(wrk_id), Some(ans), Some(new Date()))
  }
  def copy_as_timeout() = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.TIMEOUT, from_memo, worker_id, answer, Some(new Date()))
  }
  def copy_as_processed() = {
    assert(completed_at != None)
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.PROCESSED, from_memo, worker_id, answer)
  }
  def copy_as_cancelled() = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.CANCELLED, from_memo, worker_id, answer, Some(new Date()))
  }
  def copy_as_accepted() = {
    assert(completed_at != None)
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.ACCEPTED, from_memo, worker_id, answer)
  }
  def copy_as_rejected() = {
    assert(completed_at != None)
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.REJECTED, from_memo, worker_id, answer)
  }
  override def toString = {
    val has_answer = answer match { case Some(_) => "yes"; case None => "no" }
    val wid = worker_id match { case Some(wid) => wid; case None => "n/a" }
    "Thunk(state: " + state + ", has answer: " + has_answer + ", completed by worker_id: " + wid + ")"
  }
  Utilities.DebugLog("New Thunk, will expire on: " + expires_at.toString, LogLevel.INFO, LogType.SCHEDULER, computation_id)
}
