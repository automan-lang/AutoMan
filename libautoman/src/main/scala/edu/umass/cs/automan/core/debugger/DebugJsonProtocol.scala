package edu.umass.cs.automan.core.debugger

import spray.json.{ JsonFormat, DefaultJsonProtocol }

// Derived from:
// https://bitbucket.org/btamaskar/automan-debugger/src/d25b09c775336d442d2e93a1a589a42816871a81/Schema.js?at=default

case class Info(task_name: String,
                task_question: String,
                task_description: String,
                task_type: String,
                start_time: Long,
                confidence_level: Double,
                avg_task_time_sec: Double,
                avg_time_similar_tasks_sec: Double,
                answers_received: Int,
                total_answers_needed: Int)
case class PrevTimeout(timeout_num: Int,
                       timeout_time: Long,
                       identical_ans_received: Int,
                       identical_ans_shortage: Int)
case class CurrentTask(answer_num: Int,
                       time_answered: Long,
                       answer: String)
case class BudgetInfo(payout_per_task_usd: Double,
                      universal_budget_usd: Double,
                      task_budget_usd: Double,
                      budget_used: Double,
                      budget_remaining: Double)
case class Task(info: Info,
                prev_timeouts: Array[PrevTimeout],
                current_tasks: Array[CurrentTask],
                budget_info: BudgetInfo)
case class Tasks(tasks: Array[Task])

object DebugJsonProtocol extends DefaultJsonProtocol {
  implicit val infoFormat = jsonFormat10(Info.apply)
  implicit val prevTimeoutFormat = jsonFormat4(PrevTimeout.apply)
  implicit val currentTaskFormat = jsonFormat3(CurrentTask.apply)
  implicit val budgetInfoFormat = jsonFormat5(BudgetInfo.apply)
  implicit val taskFormat = jsonFormat4(Task.apply)
  implicit val tasksFormat = jsonFormat1(Tasks.apply)
}