package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.question.Question
import java.util.{UUID, Calendar, Date}
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

class Thunk(val question: Question, val timeout_in_s: Int, val worker_timeout: Int, val cost: BigDecimal, val computation_id: UUID) {
  val created_at: Date = new Date()
  var _state = SchedulerState.READY
  var answer : Answer = null
  var is_dual: Boolean = false
  var from_memo: Boolean = false
  var worker_id: Option[String] = None
  val expires_at : Date = {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.SECOND, timeout_in_s)
    calendar.getTime
  }
  def is_timedout: Boolean = {
    val now = new Date()
    expires_at.before(now)
  }
  def state: SchedulerState.Value = _state
  def state_=(s: SchedulerState.Value) { _state = s }

  override def toString = {
    val has_answer = if (answer == null) "no" else "yes"
    val wid = if (answer == null) "n/a" else worker_id.toString
    "Thunk(state: " + state + ", has answer: " + has_answer + ", completed by worker_id: " + wid + ")"
  }
  Utilities.DebugLog("New Thunk, will expire on: " + expires_at.toString, LogLevel.INFO, LogType.SCHEDULER, computation_id)
}
