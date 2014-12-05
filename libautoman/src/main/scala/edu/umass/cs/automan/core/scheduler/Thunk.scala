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
                         val answer: Option[A]
                        ) {
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

  def copy_with_state(s: SchedulerState.Value) = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, s, from_memo, worker_id, answer)
  }
  def copy_with_answer(a: A, w: String) = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.RETRIEVED, from_memo, Some(w), Some(a))
  }
  def copy_with_timeout() = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.TIMEOUT, from_memo, worker_id, answer)
  }
  def copy_with_processed() = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.PROCESSED, from_memo, worker_id, answer)
  }
  def copy_with_cancellation() = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.CANCELLED, from_memo, worker_id, answer)
  }
  def copy_with_accept() = {
    new Thunk[A](thunk_id, question, timeout_in_s, worker_timeout, cost, computation_id, created_at, SchedulerState.ACCEPTED, from_memo, worker_id, answer)
  }

  override def toString = {
    val has_answer = answer match { case Some(_) => "yes"; case None => "no" }
    val wid = worker_id match { case Some(wid) => wid; case None => "n/a" }
    "Thunk(state: " + state + ", has answer: " + has_answer + ", completed by worker_id: " + wid + ")"
  }
  Utilities.DebugLog("New Thunk, will expire on: " + expires_at.toString, LogLevel.INFO, LogType.SCHEDULER, computation_id)
}
