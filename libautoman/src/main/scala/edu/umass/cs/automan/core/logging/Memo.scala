package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.info.QuestionType._
import edu.umass.cs.automan.core.logging.tables.{DBRadioButtonAnswer, DBThunkHistory}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.SchedulerState._
import edu.umass.cs.automan.core.scheduler.Thunk
import scala.slick.driver.DerbyDriver.simple._
import scala.slick.jdbc.meta.MTable

class Memo(log_config: LogConfig.Value) {
  // typedefs
  type DBThunk = (UUID, UUID, BigDecimal, Date, Int, Int)
  type DBThunkHistory =(UUID, Date, SchedulerState)
  type DBQuestion = (UUID, String, QuestionType)
  type DBRadioButtonAnswer = (Int, Symbol, String)

  // connection string
  private val jdbc_conn_string = "jdbc:derby:AutoManMemoDB;create=true"

  // tables
  private val dbThunk = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunk]
  private val dbThunkHistory = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunkHistory]
  private val dbQuestion = TableQuery[edu.umass.cs.automan.core.logging.tables.DBQuestion]
  private val dbRadioButtonAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBRadioButtonAnswer]

  // get DB handle
  private val db = Database.forURL(jdbc_conn_string, driver = "scala.slick.driver.DerbyDriver")

  // Thunk existence cache
  private val all_thunk_ids = scala.collection.mutable.Set[UUID]()

  // create database tables if they do not already exist;
  // the database itself is automatically created by the driver
  val tables = db.withSession { implicit session => MTable.getTables(None, None, None, None).list.map(_.name.name) }
  if (!tables.contains(dbQuestion.baseTableRow.tableName)) {
    db.withSession { implicit s =>
      (dbThunk.ddl ++ dbThunkHistory.ddl ++ dbQuestion.ddl ++ dbRadioButtonAnswer.ddl).create
    }
  } else {
    db withSession { implicit s =>
      // add all of the thunk_ids from the database to the tracked set
      dbThunk.map(_.thunk_id).foreach(all_thunk_ids.add(_))
    }
  }

  /**
   * Restore all Thunks from the database given a question's memo_hash.
   * @param q An AutoMan question.
   * @tparam A The data type of the Answer.
   * @return A list of Thunks.
   */
  def restore[A](q: Question[A]) : List[Thunk[A]] = {
    implicit val javaUtilDateMapper = DBThunkHistory.javaUtilDateMapper
    implicit val symbolStringMapper = DBRadioButtonAnswer.symbolStringMapper

    // subquery: get thunk_id -> most recent state change time
    val MSQ =  dbThunkHistory.groupBy(_.thunk_id).map{ case (thunk_id,row) => thunk_id -> row.map(_.state_change_time).max }

    // get latest thunk histories
    val THS = for {
      th <- dbThunkHistory
      m <- MSQ
      if th.thunk_id === m._1 && th.state_change_time === m._2
    } yield th

    // join with thunk
    val TS_THS = dbThunk join THS on (_.thunk_id === _.thunk_id)

    // join with question
    val QS_TS_THS = dbQuestion join TS_THS on (_.id === _._1.question_id)

    // filter by memo_hash
    val fQS_TS_THS = QS_TS_THS.filter(_._1.memo_hash === q.memo_hash)

    // LEFT join with answers
    // ((DBQuestion, (DBThunk, DBThunkHistory)), DBAnswerKind)
    val A_QS_TS_THS = (fQS_TS_THS leftJoin dbRadioButtonAnswer on (_._2._2.history_id === _.history_id)).map {
      case ((dbquestion, (dbthunk, dbthunkhistory)), dbradiobuttonanswer) =>
        ( dbthunk.thunk_id,
          dbthunk.timeout_in_s,
          dbthunk.worker_timeout_in_s,
          dbthunk.cost_in_cents,
          dbthunk.creation_time,
          dbthunkhistory.scheduler_state,
          true,
          dbradiobuttonanswer.worker_id.?,
          dbradiobuttonanswer.answer.?,
          dbthunkhistory.state_change_time
        )
    }

    // execute query
    val results = db.withSession { implicit s => A_QS_TS_THS.list }

    // make and return thunks
    results.map {
      case (thunk_id, timeout_in_s, worker_timeout_in_s, cost, created_at, state, from_memo, worker_id_opt, answer_opt, state_changed_at) =>
        Thunk[A](thunk_id, q, timeout_in_s, worker_timeout_in_s, cost, created_at, state, from_memo = true, worker_id_opt, answer_opt.asInstanceOf[Option[A]], Some(state_changed_at))
    }
  }

  /**
   * Updates the database given a complete list of Thunks.
   * @param thunks A list of Thunks.
   * @tparam A The data type of the Answer.
   */
  def save[A](thunks: List[Thunk[A]]) : Unit = {
    ???
  }
}
