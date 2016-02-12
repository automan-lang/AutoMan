package edu.umass.cs.automan.adapters.mturk

import java.util.{Calendar, Date, UUID}

import com.amazonaws.mturk.requester.AssignmentStatus
import edu.umass.cs.automan.adapters.mturk.logging.MTMemo
import edu.umass.cs.automan.adapters.mturk.logging.tables.DBAssignment
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.logging.tables.{DBTaskHistory, DBTask, DBQuestion}
import edu.umass.cs.automan.core.scheduler.SchedulerState
import edu.umass.cs.automan.core.scheduler.SchedulerState.SchedulerState
import edu.umass.cs.automan.core.scheduler.SchedulerState.SchedulerState
import scala.slick.driver.H2Driver.simple._

object AcceptAllFromDB extends App {
  // parse args
  val conf = Conf(args)

  println("start")

  // TableQuery aliases
  val dbTask = TableQuery[edu.umass.cs.automan.core.logging.tables.DBTask]
  val dbTaskHistory = TableQuery[edu.umass.cs.automan.core.logging.tables.DBTaskHistory]
  val dbQuestion = TableQuery[edu.umass.cs.automan.core.logging.tables.DBQuestion]
  val dbCheckboxAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBCheckboxAnswer]
  val dbEstimationAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBEstimationAnswer]
  val dbFreeTextAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBFreeTextAnswer]
  val dbRadioButtonAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBRadioButtonAnswer]
  val dbAssignment = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBAssignment]
  val dbHIT = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBHIT]
  val dbHITType = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBHITType]
  val dbQualReq = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBQualificationRequirement]
  val dbTaskHIT = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBTaskHIT]
  val dbWorker = TableQuery[edu.umass.cs.automan.adapters.mturk.logging.tables.DBWorker]

  println("read memo DB")

  // memo db
  val memo = new MTMemo(LogConfig.TRACE_MEMOIZE_VERBOSE, conf.database_path(), false)

  println("init MTurkAdapter")

  // init adapter with no memo
  val a = MTurkAdapter { mt =>
    mt.access_key_id = conf.key()
    mt.secret_access_key = conf.secret()
    mt.sandbox_mode = conf.sandbox()
    mt.logging = LogConfig.NO_LOGGING
  }

  // get requester service handle
  val srv = a.requesterService match {
    case Some(x) => x
    case None =>
      println("Can't get handle to requester service.")
      sys.exit(1)
  }

  println("Connect to DB...")

  val db = memo.db_opt match {
    case Some(db) =>
      println("Connected to DB.")
      db
    case None =>
      println("Cannot connect to DB " + conf.database_path())
      sys.exit(1)
  }

  println("query DB")

  // get all questions
  val tasks = db.withSession { implicit s =>
    memo.allTasksQuery().list
  }

  val tMap = tasks.map { case (
    (question_id, memo_hash, question_type, text, title),
    (
      (task_id, _, round, cost, creation_time, timeout_in_s, worker_timeout_in_s),
      (history_id, _, state_change_time, scheduler_state)
      )
    ) =>
    task_id -> (question_id, round, cost, scheduler_state)
  }.toMap

  // get all assignments
  val assignments = db.withSession { implicit s =>
    dbAssignment.list
  }

  println(tasks.length + " total tasks.")
  println(assignments.length + " total assignments.")

  val aMap = assignments.groupBy { case (_,_,_,_,_,_,_,_,_,_,_,_,taskId) => taskId }

  println("accept and reject")

  var totalCost = BigDecimal(0)

  tMap.keys.foreach { task_id =>
    val (question_id: UUID, round: Int, cost: BigDecimal, schedulerState: SchedulerState) = tMap(task_id)
    val assignments = if (aMap.contains(task_id)) { aMap(task_id) } else { List() }
    assignments.foreach { case (
      assignmentId,
      workerId,
      hit_id,
      assignmentStatus,
      autoApprovalTime,
      acceptTime,
      submitTime,
      approvalTime,
      rejectionTime,
      deadline,
      answer,
      requesterFeedback,
      _) =>
        try {
          if (schedulerState != SchedulerState.ACCEPTED
            && schedulerState != SchedulerState.REJECTED
            && schedulerState != SchedulerState.CANCELLED
            && schedulerState != SchedulerState.TIMEOUT)
          srv.approveAssignment(assignmentId, "Thanks!")
          println(List(assignmentId,"success",question_id,task_id,workerId,schedulerState,assignmentStatus,round,cost).mkString(","))
          totalCost += cost
        } catch {
          case _ : Throwable =>
            println(List(assignmentId,"fail",question_id,task_id,workerId,schedulerState,assignmentStatus,round,cost).mkString(","))
        }
    }
  }

  println("total cost: $" + totalCost)

  println("done")

  a.close()

  sys.exit(0)
}
