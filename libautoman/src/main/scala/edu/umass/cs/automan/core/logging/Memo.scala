package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.info.QuestionType._
import edu.umass.cs.automan.core.logging.tables.{DBRadioButtonAnswer, DBTaskHistory}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.SchedulerState._
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import scala.slick.driver.SQLiteDriver
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.meta.MTable

object Memo {

  def sameTasks[A](ts1: List[Task], ts2: List[Task]) : Boolean = {
    val t1_map = ts1.map { t => t.task_id -> t }.toMap
    ts2.foldLeft (true) { case (acc, t) =>
      acc && t1_map.contains(t.task_id) && sameTask(t1_map(t.task_id), t)
    }
  }
  def sameTask(t1: Task, t2: Task) : Boolean = {
    // this is split into separate statements
    // to make debugging easier
    val c1 = t1.task_id == t2.task_id
    val c2 = t1.question == t2.question
    val c3 = t1.timeout_in_s == t2.timeout_in_s
    val c4 = t1.worker_timeout == t2.worker_timeout
    val c5 = t1.cost == t2.cost
    val c6 = t1.created_at == t1.created_at
    val c7 = t1.state == t2.state
    val c8 = t1.worker_id == t2.worker_id
    val c9 = t1.answer == t2.answer
    val is_same = c1 && c2 && c3 && c4 && c5 && c6 && c7 && c8 && c9
    is_same
  }
}

class Memo(log_config: LogConfig.Value) {
  // implicit conversions
  implicit val javaUtilDateMapper = DBTaskHistory.javaUtilDateMapper
  implicit val symbolStringMapper = DBRadioButtonAnswer.symbolStringMapper

  // typedefs
  type DBTask = (UUID, UUID, BigDecimal, Date, Int, Int)
  type DBTaskHistory =(UUID, Date, SchedulerState)
  type DBQuestion = (UUID, String, QuestionType)
  type DBRadioButtonAnswer = (Int, Symbol, String)
  type DBCheckboxAnswer = (Int, Set[Symbol], String)
  type DBSession = SQLiteDriver.backend.Session

  // connection string
  protected[automan] val jdbc_conn_string = "jdbc:sqlite:AutoManMemoDB"

  // TableQuery aliases
  protected[automan] val dbTask = TableQuery[edu.umass.cs.automan.core.logging.tables.DBTask]
  protected[automan] val dbTaskHistory = TableQuery[edu.umass.cs.automan.core.logging.tables.DBTaskHistory]
  protected[automan] val dbQuestion = TableQuery[edu.umass.cs.automan.core.logging.tables.DBQuestion]
  protected[automan] val dbRadioButtonAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBRadioButtonAnswer]
  protected[automan] val dbCheckboxAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBCheckboxAnswer]

  // Task cache
  protected var all_task_ids = Map[UUID,SchedulerState.Value]()

  // get DB handle
  val db_opt = log_config match {
    case LogConfig.NO_LOGGING => None
    case _ => {
      Some(Database.forURL(jdbc_conn_string, driver = "org.sqlite.JDBC"))
    }
  }

  protected[automan] def init() : Unit = {
    init_database_if_required(List())
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
  protected def init_database_if_required(ddls: List[SQLiteDriver.SchemaDescription]) : Unit = {
    val base_ddls: SQLiteDriver.DDL = dbTask.ddl ++ dbTaskHistory.ddl ++ dbQuestion.ddl ++ dbRadioButtonAnswer.ddl ++ dbCheckboxAnswer.ddl
    val all_ddls: SQLiteDriver.DDL = if (ddls.nonEmpty) {
      base_ddls ++ ddls.tail.foldLeft(ddls.head){ case (acc,ddl) => acc ++ ddl }
    } else {
      base_ddls
    }

    all_task_ids = db_opt match {
      case Some(db) => {
        if(!database_exists()) {
          // create the database
          db.withSession { implicit s => all_ddls.create }
          Map.empty
        } else {
          // prepopulate cache with all of the task_ids from the database
          db withSession { implicit s => getAllTasksMap }
        }
      }
      case None => Map.empty
    }
  }

  protected[automan] def allTasksQuery() = {
    // subquery: get task_id -> most recent state change time
    val MSQ = dbTaskHistory.groupBy(_.task_id).map{ case (task_id,row) => task_id -> row.map(_.state_change_time).max }

    // get latest task histories
    val THS = for {
    th <- dbTaskHistory
    m <- MSQ
    if th.task_id === m._1 && th.state_change_time === m._2
  } yield th

    // join with task
    val TS_THS = dbTask join THS on (_.task_id === _.task_id)

    // join with question
    dbQuestion join TS_THS on (_.id === _._1.question_id)
  }

  protected[automan] def getAllTasksMap(implicit session: DBSession) : Map[UUID,SchedulerState.Value] = {
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
    db_opt match {
      case Some(db) => {
        val QS_TS_THS = allTasksQuery()

          // filter by memo_hash
        val fQS_TS_THS = QS_TS_THS.filter(_._1.memo_hash === q.memo_hash)

          // LEFT join with answers
          // ((DBQuestion, (DBtask, DBtaskHistory)), DBAnswerKind)
        val A_QS_TS_THS = q.getQuestionType match {
          case RadioButtonQuestion => {
            (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbradiobuttonanswer) =>
                ( dbtask.task_id,
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
          case CheckboxQuestion => {
            (fQS_TS_THS leftJoin dbCheckboxAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbtask, dbtaskhistory)), dbcheckboxanswer) =>
                ( dbtask.task_id,
                  dbtask.timeout_in_s,
                  dbtask.worker_timeout_in_s,
                  dbtask.cost,
                  dbtask.creation_time,
                  dbtaskhistory.scheduler_state,
                  true,
                  dbcheckboxanswer.worker_id.?,
                  dbcheckboxanswer.answer,
                  dbtaskhistory.state_change_time
                )

            }
          }
          case _ => throw new NotImplementedError()
        }

        // execute query
        val results = db.withSession { implicit s => A_QS_TS_THS.list }.distinct

          // make and return tasks
        results.map {
          case (task_id,
                timeout_in_s,
                worker_timeout_in_s,
                cost,
                created_at,
                state,
                from_memo,
                worker_id_opt,
                answer_opt,
                state_changed_at) =>
            Task(
              task_id,
              q,
              timeout_in_s,
              worker_timeout_in_s,
              cost,
              created_at,
              state,
              from_memo = true,
              worker_id_opt,
              answer_opt.asInstanceOf[Option[Question#A]],
              state_changed_at
            )
        }
      }
      case None => List.empty
    }
  }

  protected[automan] def questionInDB(memo_hash: String)(implicit db: DBSession) : Boolean = {
    dbQuestion.filter(_.memo_hash === memo_hash).firstOption match {
      case Some(q) => true
      case None => false
    }
  }

  protected[automan] def needsUpdate[A](ts: List[Task]) : List[InsertUpdateOrSkip[Task]] = {
    ts.map { t =>
      if (!all_task_ids.contains(t.task_id)) {
        Insert(t)
      } else if (all_task_ids(t.task_id) != t.state) {
        Update(t)
      } else {
        Skip(t)
      }
    }
  }

  protected[automan] def task2TaskTuple(ts: List[Task]) : List[(UUID, UUID, BigDecimal, Date, Int, Int)] = {
    ts.map(t => (t.task_id, t.question.id, t.cost, t.created_at, t.timeout_in_s, t.worker_timeout))
  }

  protected[automan] def task2TaskHistoryTuple(ts: List[Task]) : List[(Int, UUID, Date, SchedulerState)] = {
    ts.map(t => (1, t.task_id, new Date(), t.state))
  }

  protected[automan] def task2TaskAnswerTuple(ts: List[Task], histories: Seq[(UUID, Int)]) : List[(Int, Question#A, String)] = {
    val history_dict = histories.toMap
    ts.flatMap { t =>
      t.answer match {
        case Some(ans) =>
          assert(history_dict.contains(t.task_id))
          assert(t.worker_id != None)
          Some(history_dict(t.task_id), ans, t.worker_id.get)
        case None => None
      }
    }
  }

  protected[automan] def insertAnswerTable(ts: List[Task], histories: Seq[(UUID, Int)])(implicit session: DBSession) = {
    assert(ts.size != 0)
    ts.head.question.getQuestionType match {
      case RadioButtonQuestion =>
        dbRadioButtonAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBRadioButtonAnswer]]
      case RadioButtonDistributionQuestion => ???
      case CheckboxQuestion =>
        dbCheckboxAnswer ++= task2TaskAnswerTuple(ts, histories).asInstanceOf[List[DBCheckboxAnswer]]
      case CheckboxDistributionQuestion => ???
      case FreeTextQuestion => ???
      case FreeTextDistributionQuestion => ???
    }
  }

  /**
   * Updates the database given a complete list of tasks.
   * @param ts A list of tasks.
   */
  def save(q: Question, ts: List[Task]) : Unit = {
    if(ts.size == 0) return

    db_opt match {
      case Some(db) =>
        db.withTransaction { implicit session =>
          synchronized {
            // is the question even in the database?
            if (!questionInDB(q.memo_hash)) {
                // create dbQuestion record for this memo_hash
                dbQuestion += (q.id, q.memo_hash, q.getQuestionType)
            }

            // determine which records need to be inserted/updated/ignored
            val (inserts, updates) = needsUpdate (ts).foldLeft ((List.empty[Task], List.empty[Task] ) ) {
              case (acc, ius) => ius match {
                case Insert(t) => (t :: acc._1, acc._2)
                case Update(t) => (acc._1, t :: acc._2)
                case Skip(t) => acc
              }
            }

            // do bulk insert for new tasks
            dbTask ++= task2TaskTuple(inserts)

            // do bulk insert for all task histories (inserts and updates)
            dbTaskHistory ++= task2TaskHistoryTuple(inserts ::: updates)

            // Derby can only return a single item, so we were not able to
            // get a task_id -> history_map in the previous step;
            // instead we query the table we just inserted into
            val t_ids = ts.map(_.task_id).toSet
            val histories = dbTaskHistory.filter(_.task_id inSet t_ids).map(th => (th.task_id, th.history_id)).list

            // do bulk insert for all answered tasks
            insertAnswerTable(ts, histories)

            // update the cache
            all_task_ids = ts.map { t =>
              t.task_id -> t.state
            }.toMap
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
        if (database_exists()) {
          db.withTransaction { implicit session =>
            dbQuestion.delete
            dbTask.delete
            dbTaskHistory.delete
            dbRadioButtonAnswer.delete
          }
        }
      case None => ()
    }
  }
}
