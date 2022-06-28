package org.automanlang.core.scheduler

import org.automanlang.core.logging.{LogLevelInfo, LogType, DebugLog}
import org.automanlang.core.question.Question
import java.util.{UUID, Calendar, Date}

case class Task(task_id: UUID,
                question: Question,
                round: Int,
                timeout_in_s: Int,
                worker_timeout: Int,
                cost: BigDecimal,
                created_at: Date,
                state: SchedulerState.Value,
                from_memo: Boolean,
                worker_id: Option[String],
                answer: Option[Question#A],
                csv_written: Boolean,  // whether the task has been written to CSV in schSaveCSV by Scheduler
                state_changed_at: Date) {
  val expires_at : Date = {
    val calendar = Calendar.getInstance()
    calendar.setTime(created_at)
    calendar.add(Calendar.SECOND, timeout_in_s)
    calendar.getTime
  }

  // todo clean up getter and setter
  var _answer: Option[Question#A] = answer

  def answer(ans: Option[Question#A]): Unit = {_answer = ans}

  if(state == SchedulerState.READY) {
    DebugLog("New Task " + task_id.toString + "; with cost: $" + cost.toString() + "; will expire at: " + expires_at.toString, LogLevelInfo(), LogType.SCHEDULER, question.id)
  }

  def getID: UUID = {
    task_id
  }

  def prettyPrintAnswer: String = {
    this._answer match {
      case Some(a) => question.prettyPrintAnswer(a.asInstanceOf[Task.this.question.A])
      case None => "n/a"
    }

  }

  def is_timedout(current_time: Date): Boolean = {
    expires_at.before(current_time)
  }
  def copy_as_running(): Task = {
    DebugLog("Task " + task_id.toString +  " changed to RUNNING state; will expire at: " + expires_at.toString, LogLevelInfo(), LogType.SCHEDULER, question.id)
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, SchedulerState.RUNNING, from_memo, worker_id, answer, csv_written, new Date())
  }
  def copy_with_answer(ans: Question#A, wrk_id: String): Task = {
    _answer = Option(ans)
    DebugLog("Task " + task_id.toString +  " changed to ANSWERED state with cost \"" + cost +"\" and answer \"" + prettyPrintAnswer + "\"", LogLevelInfo(), LogType.SCHEDULER, question.id)
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, SchedulerState.ANSWERED, from_memo, Some(wrk_id), Some(ans), csv_written, new Date())
  }
  def copy_as_csv_written(): Task = {
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, state, from_memo, worker_id, answer, csv_written = true, new Date())
  }
  def copy_as_duplicate(): Task = {
    DebugLog("Task " + task_id.toString +  " changed to DUPLICATE state with answer \"" + prettyPrintAnswer + "\" for worker_id = \"" + worker_id + "\"", LogLevelInfo(), LogType.SCHEDULER, question.id)
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, SchedulerState.DUPLICATE, from_memo, worker_id, answer, csv_written, new Date())
  }
  def copy_as_timeout(): Task = {
    DebugLog("Task " + task_id.toString +  " changed to TIMEOUT state; expired at: " + expires_at.toString, LogLevelInfo(), LogType.SCHEDULER, question.id)
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, SchedulerState.TIMEOUT, from_memo, worker_id, answer, csv_written, new Date())
  }
  def copy_as_cancelled(): Task = {
    DebugLog("Task " + task_id.toString +  " changed to CANCELLED state.", LogLevelInfo(), LogType.SCHEDULER, question.id)
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, SchedulerState.CANCELLED, from_memo, worker_id, answer, csv_written, new Date())
  }
  def copy_as_accepted(): Task = {
    DebugLog("Task " + task_id.toString +  " changed to ACCEPTED state.", LogLevelInfo(), LogType.SCHEDULER, question.id)
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, SchedulerState.ACCEPTED, from_memo, worker_id, answer, csv_written, new Date())
  }
  def copy_as_rejected(): Task = {
    DebugLog("Task " + task_id.toString +  " changed to REJECTED state.", LogLevelInfo(), LogType.SCHEDULER, question.id)
    Task(task_id, question, round, timeout_in_s, worker_timeout, cost, created_at, SchedulerState.REJECTED, from_memo, worker_id, answer, csv_written, new Date())
  }
  override def toString: String = {
    val has_answer = answer match { case Some(_) => "yes"; case None => "no" }
    val wid = worker_id match { case Some(wid) => wid; case None => "n/a" }
    "Task(id: " + task_id + ", state: " + state + ", has answer: " + has_answer + ", completed by worker_id: " + wid + ")"
  }
}
