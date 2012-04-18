package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.question.Question
import java.util.{Calendar, Date}
import edu.umass.cs.automan.core.answer.Answer

class Thunk(val question: Question, val timeout_in_s: Int, val worker_timeout: Int, val cost: BigDecimal) {
  var _state = SchedulerState.READY
  var answer : Answer = null
  var is_dual: Boolean = false
  var from_memo: Boolean = false
  val expires_at : Date = {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.SECOND, timeout_in_s)
    calendar.getTime
  }
  def comparator = answer.comparator
  def is_timedout: Boolean = {
    val now = new Date()
    expires_at.before(now)
  }
  def state: SchedulerState.Value = _state
  def state_=(s: SchedulerState.Value) { _state = s }

  override def toString = {
    val has_answer = if (answer == null) "no" else "yes"
    val worker_id = if (answer == null) "n/a" else answer.worker_id
    "Thunk(state: " + state + ", has answer: " + has_answer + ", completed by worker_id: " + worker_id + ")"
  }
  
  println("DEBUG: THUNK: New Thunk, will expire on: " + expires_at.toString)
}
