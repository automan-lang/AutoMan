package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.info.QuestionType._
import edu.umass.cs.automan.core.logging.tables.{DBRadioButtonAnswer, DBThunkHistory}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.SchedulerState._
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import scala.slick.driver.DerbyDriver.simple._
import scala.slick.jdbc.meta.MTable

object Memo {
  def sameThunks[A](ts1: List[Thunk[A]], ts2: List[Thunk[A]]) : Boolean = {
    val t1_map = ts1.map { t => t.thunk_id -> t }.toMap
    ts2.foldLeft (true) { case (acc, t) =>
      acc &&
        t1_map.contains(t.thunk_id) &&
        sameThunk(t1_map(t.thunk_id), t)
    }
  }
  def sameThunk[A](t1: Thunk[A], t2: Thunk[A]) : Boolean = {
    t1.thunk_id == t2.thunk_id &&
      t1.question == t2.question &&
      t1.timeout_in_s == t2.timeout_in_s &&
      t1.worker_timeout == t2.worker_timeout &&
      t1.cost == t2.cost &&
      t1.created_at == t1.created_at &&
      t1.state == t2.state &&
      t1.worker_id == t2.worker_id &&
      t1.answer == t2.answer &&
      t1.completed_at == t2.completed_at
  }
}

class Memo(log_config: LogConfig.Value) {
  // implicit conversions
  implicit val javaUtilDateMapper = DBThunkHistory.javaUtilDateMapper
  implicit val symbolStringMapper = DBRadioButtonAnswer.symbolStringMapper

  // typedefs
  type DBThunk = (UUID, UUID, BigDecimal, Date, Int, Int)
  type DBThunkHistory =(UUID, Date, SchedulerState)
  type DBQuestion = (UUID, String, QuestionType)
  type DBRadioButtonAnswer = (Int, Symbol, String)

  // connection string
  private val jdbc_conn_string = "jdbc:derby:AutoManMemoDB;create=true"

  // TableQuery aliases
  private val dbThunk = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunk]
  private val dbThunkHistory = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunkHistory]
  private val dbQuestion = TableQuery[edu.umass.cs.automan.core.logging.tables.DBQuestion]
  private val dbRadioButtonAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBRadioButtonAnswer]

  // get DB handle
  val db_opt = log_config match {
    case LogConfig.NO_LOGGING => None
    case _ => {
      Some(Database.forURL(jdbc_conn_string, driver = "scala.slick.driver.DerbyDriver"))
    }
  }

  // create database tables if they do not already exist
  // (the database itself is automatically created by the driver);
  // and then populate thunk cache
  private var all_thunk_ids: Map[UUID,SchedulerState.Value] = db_opt match {
    case Some(db) => {
      val tables = db.withSession { implicit session => MTable.getTables(None, None, None, None).list.map(_.name.name)}
      if (!tables.contains(dbQuestion.baseTableRow.tableName)) {
        db.withSession { implicit s =>
          (dbThunk.ddl ++ dbThunkHistory.ddl ++ dbQuestion.ddl ++ dbRadioButtonAnswer.ddl).create
        }
        Map.empty
      } else {
        db withSession { implicit s =>
          // prepopulate cache with all of the thunk_ids from the database
          getAllThunksMap
        }
      }
    }
    case None => Map.empty
  }

  private def allThunksQuery() = {
    // subquery: get thunk_id -> most recent state change time
    val MSQ = dbThunkHistory.groupBy(_.thunk_id).map{ case (thunk_id,row) => thunk_id -> row.map(_.state_change_time).max }

    // get latest thunk histories
    val THS = for {
    th <- dbThunkHistory
    m <- MSQ
    if th.thunk_id === m._1 && th.state_change_time === m._2
  } yield th

    // join with thunk
    val TS_THS = dbThunk join THS on (_.thunk_id === _.thunk_id)

    // join with question
    dbQuestion join TS_THS on (_.id === _._1.question_id)
  }

  private def getAllThunksMap : Map[UUID,SchedulerState.Value] = {
    allThunksQuery().map { case (dbquestion, (dbthunk, dbthunkhistory)) =>
      dbthunk.thunk_id -> dbthunkhistory.scheduler_state
    }.list.toMap
  }

  /**
   * Restore all Thunks from the database given a question's memo_hash.
   * @param q An AutoMan question.
   * @tparam A The data type of the Answer.
   * @return A list of Thunks.
   */
  def restore[A](q: Question[A]) : List[Thunk[A]] = {
    db_opt match {
      case Some(db) => {
        val QS_TS_THS = allThunksQuery()

          // filter by memo_hash
        val fQS_TS_THS = QS_TS_THS.filter(_._1.memo_hash === q.memo_hash)

          // LEFT join with answers
          // ((DBQuestion, (DBThunk, DBThunkHistory)), DBAnswerKind)
        val A_QS_TS_THS = q.getQuestionType match {
          case RadioButtonQuestion => {
            (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
              case ((dbquestion, (dbthunk, dbthunkhistory)), dbradiobuttonanswer) =>
                ( dbthunk.thunk_id,
                  dbthunk.timeout_in_s,
                  dbthunk.worker_timeout_in_s,
                  dbthunk.cost,
                  dbthunk.creation_time,
                  dbthunkhistory.scheduler_state,
                  true,
                  dbradiobuttonanswer.worker_id.?,
                  dbradiobuttonanswer.answer.?,
                  dbthunkhistory.state_change_time
                )
            }
          }
          case _ => throw new NotImplementedError()
        }

          // execute query
        val results = db.withSession { implicit s => A_QS_TS_THS.list }

          // make and return thunks
        results.map {
        case (thunk_id, timeout_in_s, worker_timeout_in_s, cost, created_at, state, from_memo, worker_id_opt, answer_opt, state_changed_at) =>
        Thunk[A](thunk_id, q, timeout_in_s, worker_timeout_in_s, cost, created_at, state, from_memo = true, worker_id_opt, answer_opt.asInstanceOf[Option[A]], Some(state_changed_at))
        }
      }
      case None => List.empty
    }
  }

  private def needsUpdate[A](ts: List[Thunk[A]]) : List[InsertUpdateOrSkip[A]] = {
    ts.map { t =>
      if (!all_thunk_ids.contains(t.thunk_id)) {
        Insert(t)
      } else if (all_thunk_ids(t.thunk_id) != t.state) {
        Update(t)
      } else {
        Skip(t)
      }
    }
  }

  /**
   * Updates the database given a complete list of Thunks.
   * @param ts A list of Thunks.
   * @tparam A The data type of the Answer.
   */
  def save[A](q: Question[A], ts: List[Thunk[A]]) : Unit = {
    synchronized {
      // determine which records need to be updated
      val (inserts,updates) = needsUpdate(ts).foldLeft((List.empty[Thunk[A]],List.empty[Thunk[A]])) {
        case (acc, ius) => ius match {
          case Insert(t) => (t :: acc._1, acc._2)
          case Update(t) => (acc._1, t :: acc._2)
          case Skip(t) => acc
        }
      }

      // do thunk insert

      // do thunk history insert

      // do answer insert
      
      ???
    }
  }
}
