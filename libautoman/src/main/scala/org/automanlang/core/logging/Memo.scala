package org.automanlang.core.logging

import java.io.File
import java.util.{Date, UUID}

import org.automanlang.core.Plugin
import org.automanlang.core.info.QuestionType
import org.automanlang.core.info.QuestionType._
import org.automanlang.core.logging.tables._
import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.SchedulerState._
import org.automanlang.core.scheduler.{SchedulerState, Task}
import org.automanlang.core.Plugin

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.meta.MTable

object Memo {

  def sameTasks[A](ts1: List[Task], ts2: List[Task]) : Boolean = {
    val t1_map = ts1.map { t => t.task_id -> t }.toMap
    ts1.size == ts2.size &&                   // same size
      ts1.nonEmpty &&                         // non-empty
      ts2.foldLeft (true) { case (acc, t) =>  // all the elements are the same
      acc && t1_map.contains(t.task_id) && sameTask(t1_map(t.task_id), t)
    }
  }
  def sameTask(t1: Task, t2: Task) : Boolean = {
    // this is split into separate statements
    // to make debugging easier
    val c1 = t1.task_id == t2.task_id
    val c2 = t1.question == t2.question
    val c3 = t1.round == t2.round
    val c4 = t1.timeout_in_s == t2.timeout_in_s
    val c5 = t1.worker_timeout == t2.worker_timeout
    val c6 = t1.cost == t2.cost
    val c7 = t1.created_at == t1.created_at
    val c8 = t1.state == t2.state
    val c9 = t1.worker_id == t2.worker_id
    val c10 = t1.answer == t2.answer
    val is_same = c1 && c2 && c3 && c4 && c5 && c6 && c7 && c8 && c9 && c10
    is_same
  }
}

class Memo(log_config: LogConfig.Value, database_name: String, in_mem_db: Boolean) {
  // implicit conversions
  implicit val javaUtilDateMapper = DBTaskHistory.javaUtilDateMapper
  implicit val symbolStringMapper = DBRadioButtonAnswer.symbolStringMapper
  implicit val symbolSetStringMapper = DBCheckboxAnswer.symbolSetStringMapper
  implicit val symbolSetTupleStringMapper = DBHugoAnswer.symbolSetTupleStringMapper
  implicit val questionTypeMapper = DBQuestion.questionTypeMapper
  implicit val estimateArrayMapper = DBMultiEstimationAnswer.estimateArrayMapper

  // typedefs
  type DBTask = (UUID, UUID, BigDecimal, Date, Int, Int)
  type DBTaskHistory =(UUID, Date, SchedulerState)
  type DBQuestion = (UUID, String, QuestionType, String, String)
  type DBRadioButtonAnswer = (Int, Symbol, String)
  type DBCheckboxAnswer = (Int, Set[Symbol], String)
  type DBHugoAnswer = (Int, (Set[Symbol], Set[Symbol]), String)
  type DBEstimationAnswer = (Int, Double, String)
  type DBFreeTextAnswer = (Int, String, String)
  type DBMultiEstimationAnswer = (Int, Array[Double], String)
  type DBSession = H2Driver.backend.Session
  //type DBSurvey = ()

  // canonical path
  val path = new File(database_name.replace(".mv.db","")).getCanonicalPath

  // connection string
  protected[automanlang] val _jdbc_conn_string = if (in_mem_db) { "jdbc:h2:mem:" } else { "jdbc:h2:" + path }

  // TableQuery aliases
  protected[automanlang] val dbTask = TableQuery[org.automanlang.core.logging.tables.DBTask]
  protected[automanlang] val dbTaskHistory = TableQuery[org.automanlang.core.logging.tables.DBTaskHistory]
  protected[automanlang] val dbQuestion = TableQuery[org.automanlang.core.logging.tables.DBQuestion]
  protected[automanlang] val dbHugoAnswer = TableQuery[org.automanlang.core.logging.tables.DBHugoAnswer]
  protected[automanlang] val dbCheckboxAnswer = TableQuery[org.automanlang.core.logging.tables.DBCheckboxAnswer]
  protected[automanlang] val dbEstimationAnswer = TableQuery[org.automanlang.core.logging.tables.DBEstimationAnswer]
  protected[automanlang] val dbFreeTextAnswer = TableQuery[org.automanlang.core.logging.tables.DBFreeTextAnswer]
  protected[automanlang] val dbMultiEstimationAnswer = TableQuery[org.automanlang.core.logging.tables.DBMultiEstimationAnswer]
  protected[automanlang] val dbRadioButtonAnswer = TableQuery[org.automanlang.core.logging.tables.DBRadioButtonAnswer]
  protected[automanlang] val dbSurvey = null
  protected[automanlang] val dbVariantAnswer = null

  // registered plugins
  protected var _plugins = List[Plugin]()

  // task state cache
  private var _task_state_cache = Map[UUID,SchedulerState]()

  // get DB handle
  protected var db_opt = log_config match {
    case LogConfig.NO_LOGGING => None
    case _ => {
      Some(Database.forURL(_jdbc_conn_string, driver = "org.h2.Driver"))
    }
  }

  /**
   * Initialization routines.
   */
  protected[automanlang] def init() : Unit = {
    init_database_if_required(List())
  }

  /**
   * Register plugins so that they can be notified on DB state changes.
   * @param plugins A list of initialized plugins.
   */
  protected[automanlang] def register_plugins(plugins: List[Plugin]): Unit = {
    _plugins = plugins
  }

  protected def database_exists() : Boolean = {
    db_opt match {
      case Some(db) => {
        val tables = db.withSession { implicit session =>
          MTable.getTables(None, None, None, None).list.map(_.name.name)
        }
        if (!tables.contains(dbQuestion.baseTableRow.tableName)) {
          false
        } else {
          true
        }
      }
      case None => true
    }
  }

  /**
   * Run table definitions if this is the first time the database is run.  Overring subclasses
   * should provide their DDLs as a list to this method instead of overriding it.
   * @param ddls Slick Table definitions.
   */
  protected def init_database_if_required(ddls: List[H2Driver.SchemaDescription]) : Unit = {
    val base_ddls: H2Driver.DDL =
      dbTask.ddl ++
      dbTaskHistory.ddl ++
      dbQuestion.ddl ++
      dbCheckboxAnswer.ddl ++
      dbHugoAnswer.ddl ++
      dbEstimationAnswer.ddl ++
      dbFreeTextAnswer.ddl ++
      dbRadioButtonAnswer.ddl ++
      //dbSurvey.ddl ++
      dbMultiEstimationAnswer.ddl

    val all_ddls: H2Driver.DDL = if (ddls.nonEmpty) {
      base_ddls ++ ddls.tail.foldLeft(ddls.head){ case (acc,ddl) => acc ++ ddl }
    } else {
      base_ddls
    }

    db_opt match {
      case Some(db) => {
        if(!database_exists()) {
          // create the database
          db.withSession { implicit s => all_ddls.create }
        }
      }
      case None => ()
    }
  }

  private def restore_task_snapshots_of_type(qt: QuestionType.Value)(implicit s: DBSession) : List[TaskSnapshot[_]] = {
    qt match {
      case QuestionType.CheckboxQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbCheckboxAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
          ( t.task_id,
            q.id,
            q.title,
            q.text,
            t.round,
            t.timeout_in_s,
            t.worker_timeout_in_s,
            t.cost,
            t.creation_time,
            th.scheduler_state,
            h.worker_id.?,
            h.answer.?,
            th.state_change_time,
            QuestionType.CheckboxQuestion)
          }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.HugoQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbHugoAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
            ( t.task_id,
              q.id,
              q.title,
              q.text,
              t.round,
              t.timeout_in_s,
              t.worker_timeout_in_s,
              t.cost,
              t.creation_time,
              th.scheduler_state,
              h.worker_id.?,
              h.answer.?,
              th.state_change_time,
              QuestionType.HugoQuestion)
          }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.CheckboxDistributionQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbCheckboxAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
          ( t.task_id,
            q.id,
            q.title,
            q.text,
            t.round,
            t.timeout_in_s,
            t.worker_timeout_in_s,
            t.cost,
            t.creation_time,
            th.scheduler_state,
            h.worker_id.?,
            h.answer.?,
            th.state_change_time,
            QuestionType.CheckboxDistributionQuestion)
        }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.EstimationQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbEstimationAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
            ( t.task_id,
              q.id,
              q.title,
              q.text,
              t.round,
              t.timeout_in_s,
              t.worker_timeout_in_s,
              t.cost,
              t.creation_time,
              th.scheduler_state,
              h.worker_id.?,
              h.answer.?,
              th.state_change_time,
              QuestionType.EstimationQuestion)
          }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.MultiEstimationQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbMultiEstimationAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
            ( t.task_id,
              q.id,
              q.title,
              q.text,
              t.round,
              t.timeout_in_s,
              t.worker_timeout_in_s,
              t.cost,
              t.creation_time,
              th.scheduler_state,
              h.worker_id.?,
              h.answers.?,
              th.state_change_time,
              QuestionType.MultiEstimationQuestion)
          }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.FreeTextQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbFreeTextAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
          ( t.task_id,
            q.id,
            q.title,
            q.text,
            t.round,
            t.timeout_in_s,
            t.worker_timeout_in_s,
            t.cost,
            t.creation_time,
            th.scheduler_state,
            h.worker_id.?,
            h.answer.?,
            th.state_change_time,
            QuestionType.FreeTextQuestion)
        }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.FreeTextDistributionQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbFreeTextAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
          ( t.task_id,
            q.id,
            q.title,
            q.text,
            t.round,
            t.timeout_in_s,
            t.worker_timeout_in_s,
            t.cost,
            t.creation_time,
            th.scheduler_state,
            h.worker_id.?,
            h.answer.?,
            th.state_change_time,
            QuestionType.FreeTextDistributionQuestion)
        }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.RadioButtonQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbRadioButtonAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
          ( t.task_id,
            q.id,
            q.title,
            q.text,
            t.round,
            t.timeout_in_s,
            t.worker_timeout_in_s,
            t.cost,
            t.creation_time,
            th.scheduler_state,
            h.worker_id.?,
            h.answer.?,
            th.state_change_time,
            QuestionType.RadioButtonQuestion)
        }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.RadioButtonDistributionQuestion =>
        val ts = allTasksQuery()
          .filter { case (q,(t,h)) => q.question_type === qt }
          .leftJoin(dbRadioButtonAnswer).on(_._2._2.history_id === _.history_id)
          .map { case ((q,(t,th)),h) =>
          ( t.task_id,
            q.id,
            q.title,
            q.text,
            t.round,
            t.timeout_in_s,
            t.worker_timeout_in_s,
            t.cost,
            t.creation_time,
            th.scheduler_state,
            h.worker_id.?,
            h.answer.?,
            th.state_change_time,
            QuestionType.RadioButtonDistributionQuestion)
        }.list.distinct
        ts.map { t => new TaskSnapshot(t) }
      case QuestionType.Survey =>
        //val ts = null
        //ts.map { t => null }
        List[TaskSnapshot[_]]()
    }
  }

  def snapshotUpdates() : List[TaskSnapshot[_]] = {
    snapshot()
  }

  def snapshot() : List[TaskSnapshot[_]] = {
    db_opt match {
      case Some(db) => {
        db.withSession { s =>
          restore_task_snapshots_of_type(QuestionType.CheckboxQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.HugoQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.CheckboxDistributionQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.EstimationQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.FreeTextQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.FreeTextDistributionQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.RadioButtonQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.RadioButtonDistributionQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.MultiEstimationQuestion)(s) :::
          restore_task_snapshots_of_type(QuestionType.Survey)(s) :::
          restore_task_snapshots_of_type(QuestionType.VariantQuestion)(s)
        }

      }
      case None => List.empty
    }
  }

  // get task_id -> most recent state change time
  protected[automanlang] def mostRecentHistories() = {
    val MSQ = dbTaskHistory.groupBy(_.task_id).map{ case (task_id,row) => task_id -> row.map(_.state_change_time).max }

    // get latest task histories
    for {
      th <- dbTaskHistory
      m <- MSQ
      if th.task_id === m._1 && th.state_change_time === m._2
    } yield th
  }

  protected[automanlang] def allTasksQuery() = {
    // join with task
    val TS_THS = dbTask join mostRecentHistories() on (_.task_id === _.task_id)

    // join with question
    dbQuestion join TS_THS on (_.id === _._1.question_id)
  }

  protected[automanlang] def getAllTasksMap(implicit session: DBSession) : Map[UUID,SchedulerState.Value] = {
    allTasksQuery().map { case (dbquestion, (dbtask, dbtaskhistory)) =>
      dbtask.task_id -> dbtaskhistory.scheduler_state
    }.list.toMap
  }

  /**
   * Restore all tasks from the database given a question's memo_hash.
   * @param q An AutoMan question.
   * @return A list of tasks.
   */
  def restore(q: Question) : List[Task] = {
    DebugLog(s"Called restore.",
      LogLevelDebug(),
      LogType.MEMOIZER,
      q.id
    )

    db_opt match {
      case Some(db) => {
        val QS_TS_THS = allTasksQuery()

        // filter by memo_hash
        val fQS_TS_THS = QS_TS_THS.filter(_._1.memo_hash === q.memo_hash)

        // LEFT join with answers
        // ((DBQuestion, (DBtask, DBtaskHistory)), DBAnswerKind)
        val A_QS_TS_THS = q.getQuestionType match {
          case CheckboxQuestion => {
            (fQS_TS_THS leftJoin dbCheckboxAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbcheckboxanswer) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbcheckboxanswer.worker_id.?,
                  dbcheckboxanswer.answer.?,
                  dbtaskhistory.state_change_time
                  )

            }
          }
          case HugoQuestion => {
            (fQS_TS_THS leftJoin dbHugoAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbhugoanswer) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbhugoanswer.worker_id.?,
                  dbhugoanswer.answer.?,
                  dbtaskhistory.state_change_time
                )

            }
          }
          case CheckboxDistributionQuestion => {
            (fQS_TS_THS leftJoin dbCheckboxAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbcbda) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbcbda.worker_id.?,
                  dbcbda.answer.?,
                  dbtaskhistory.state_change_time
                  )

            }
          }
          case EstimationQuestion => {
            (fQS_TS_THS leftJoin dbEstimationAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbestimationanswer) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbestimationanswer.worker_id.?,
                  dbestimationanswer.answer.?,
                  dbtaskhistory.state_change_time
                  )
            }
          }
          case MultiEstimationQuestion => {
            (fQS_TS_THS leftJoin dbMultiEstimationAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbmultiestimationanswer) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbmultiestimationanswer.worker_id.?,
                  dbmultiestimationanswer.answers.?,
                  dbtaskhistory.state_change_time
                  )
            }
          }
          case FreeTextQuestion => {
            (fQS_TS_THS leftJoin dbFreeTextAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbfreetextanswer) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbfreetextanswer.worker_id.?,
                  dbfreetextanswer.answer.?,
                  dbtaskhistory.state_change_time
                  )
            }
          }
          case FileDistributionQuestion => {
            (fQS_TS_THS leftJoin dbFreeTextAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbftda) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbftda.worker_id.?,
                  dbftda.answer.?,
                  dbtaskhistory.state_change_time
                )
            }
          }
          case FreeTextDistributionQuestion => {
            (fQS_TS_THS leftJoin dbFreeTextAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbftda) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbftda.worker_id.?,
                  dbftda.answer.?,
                  dbtaskhistory.state_change_time
                  )
            }
          }
          case RadioButtonQuestion => {
            (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbradiobuttonanswer) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbradiobuttonanswer.worker_id.?,
                  dbradiobuttonanswer.answer.?,
                  dbtaskhistory.state_change_time
                  )
            }
          }
          case RadioButtonDistributionQuestion => {
            (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbrbda) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbrbda.worker_id.?,
                  dbrbda.answer.?,
                  dbtaskhistory.state_change_time
                  )
            }
          }
          case Survey => { // todo figure out answer
            (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbsurvey) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbsurvey.worker_id.?,
                  dbsurvey.answer.?,
                  dbtaskhistory.state_change_time
                )
            }
          }
          case VariantQuestion => { // todo figure out what this actually needs
            (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbsurvey) =>
                (dbtask.task_id,
                  dbtask.round,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbsurvey.worker_id.?,
                  dbsurvey.answer.?,
                  dbtaskhistory.state_change_time
                )
            }
          }
        }

        // execute query
        val results = db.withSession { implicit s => A_QS_TS_THS.list }.distinct

        // make and return tasks
        val tasks = results.map {
          case (task_id,
                round,
                timeout_in_s,
                worker_timeout_in_s,
                cost,
                created_at,
                state,
                from_memo,
                worker_id,
                answer,
                state_changed_at) =>
            Task(
              task_id,
              q,
              round,
              timeout_in_s,
              worker_timeout_in_s,
              cost,
              created_at,
              state,
              from_memo = true,
              worker_id,
              answer.asInstanceOf[Option[Question#A]],
              state_changed_at
            )
        }

        // restore task cache
        _task_state_cache = _task_state_cache ++ tasks.map { t => t.task_id -> t.state }.toMap

        tasks
      }
      case None => List.empty
    }
  }

  protected[automanlang] def questionInDB(memo_hash: String)(implicit db: DBSession) : Boolean = {
    dbQuestion.filter(_.memo_hash === memo_hash).firstOption match {
      case Some(q) => true
      case None => false
    }
  }

  protected[automanlang] def needsUpdate[A](ts: List[Task], tsstates: Map[UUID,SchedulerState.Value]) : List[InsertUpdateOrSkip[Task]] = {
    ts.map { t =>
      if (!tsstates.contains(t.task_id)) {
        Insert(t)
      } else if (tsstates(t.task_id) != t.state) {
        Update(t)
      } else {
        Skip(t)
      }
    }
  }

  protected[automanlang] def task2TaskTuple(ts: List[Task]) : List[(UUID, UUID, Int, BigDecimal, Date, Int, Int)] = {
    ts.map(t => (t.task_id, t.question.id, t.round, t.cost, t.created_at, t.timeout_in_s, t.worker_timeout))
  }

  protected[automanlang] def task2TaskHistoryTuple(ts: List[Task]) : List[(Int, UUID, Date, SchedulerState)] = {
    ts.map(t => (1, t.task_id, new Date(), t.state))
  }

  protected[automanlang] def task2TaskAnswerTuple(ts: List[Task], histories: Seq[(UUID, Int)]) : List[(Int, Question#A, String)] = {
    val history_dict = histories.toMap
    ts.flatMap { t =>
      t.answer match {
        case Some(ans) =>
          assert(history_dict.contains(t.task_id))
          assert(t.worker_id.isDefined)
          Some(history_dict(t.task_id), ans, t.worker_id.get)
        case None => None
      }
    }
  }

  protected[automanlang] def insertAnswerTable(ts: List[Task], histories: Seq[(UUID, Int)])(implicit session: DBSession) = {
    assert(ts.nonEmpty)
    ts.head.question.getQuestionType match {
      case HugoQuestion =>
        dbHugoAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBHugoAnswer]]
      case CheckboxQuestion =>
        dbCheckboxAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBCheckboxAnswer]]
      case CheckboxDistributionQuestion =>
        dbCheckboxAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBCheckboxAnswer]]
      case EstimationQuestion =>
        dbEstimationAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBEstimationAnswer]]
      case MultiEstimationQuestion =>
        dbMultiEstimationAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBMultiEstimationAnswer]]
      case FreeTextQuestion =>
        dbFreeTextAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBFreeTextAnswer]]
      case FreeTextDistributionQuestion =>
        dbFreeTextAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBFreeTextAnswer]]
      case FileDistributionQuestion =>
        dbFreeTextAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBFreeTextAnswer]]
      case RadioButtonQuestion =>
        dbRadioButtonAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBRadioButtonAnswer]]
      case RadioButtonDistributionQuestion =>
        dbRadioButtonAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBRadioButtonAnswer]]
      case Survey =>
        dbSurvey //++= null
      case VariantQuestion =>
        dbVariantAnswer
    }
  }

  private def findNecessaryUpdates(tasks: List[Task]) : List[Task] = {
    assert(tasks.forall(t => _task_state_cache.contains(t.task_id)))

    // only update those tasks whose state has changed or are not in the map
    tasks.filter { t => _task_state_cache(t.task_id) != t.state }
  }

  /**
   * Updates the database given a complete list of tasks.
   * @param inserts A list of tasks to insert.
   * @param updates A list of tasks to update.
   */
  def save(q: Question, inserts: List[Task], updates: List[Task]) : Unit = {
    DebugLog(s"Called save.",
      LogLevelDebug(),
      LogType.MEMOIZER,
      q.id
    )

    // update cache
    _task_state_cache = _task_state_cache ++ inserts.map { t => t.task_id -> t.state}

    val necessary_updates = findNecessaryUpdates(updates)

    // update cache
    _task_state_cache = _task_state_cache ++ necessary_updates.map { t => t.task_id -> t.state}

    if(inserts.isEmpty && necessary_updates.isEmpty) return

    db_opt match {
      case Some(db) =>
        db.withSession { implicit session =>
          // is the question even in the database?
          if (!questionInDB(q.memo_hash)) {
            // create dbQuestion record for this memo_hash
            dbQuestion +=(q.id, q.memo_hash, q.getQuestionType, q.text, q.title)
          }

          // update tasks
          dbTask ++= task2TaskTuple(inserts)

          // do bulk insert for all task histories (inserts and updates)
          dbTaskHistory ++= task2TaskHistoryTuple(inserts ::: necessary_updates)

          // H2 can only return a single autoinc row, so we were not able to
          // get a task_id -> history_map in the previous step;
          // instead we query the table we just inserted into
          val ts = inserts ::: necessary_updates
          val t_ids = ts.map(_.task_id).toSet
          val histories: List[(UUID, Int)] =
            mostRecentHistories()                         // only the most recent histories
            .filter(_.task_id inSet t_ids)                // for the ones that we just inserted
            .map(th => (th.task_id, th.history_id)).list  // and we only care about task_id and history_id

          // do bulk insert for all answered tasks
          insertAnswerTable(ts, histories)

          // asynchronously send update notifications
          // this should only send changes; for now, send everything
          if (_plugins.nonEmpty) {
            Future {
              blocking {
                val updates = snapshotUpdates()
                _plugins.foreach(_.state_updates(updates))
              }
            }
          }
        }
      case None => ()
    }
  }

  /**
   * This call deletes all records stored in all of the Memo
   * database's tables.
   */
  def wipeDatabase() : Unit = {
    db_opt match {
      case Some(db) =>
        val fullpath = path + ".mv.db"
        new File(fullpath).delete()
      case None => ()
    }
  }
}
