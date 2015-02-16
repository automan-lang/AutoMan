package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.info.QuestionType._
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
  private val jdbc_conn_string = "jdbc:derby:AutoManMemoDB"

  // tables
  private val dbThunk = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunk]
  private val dbThunkHistory = TableQuery[edu.umass.cs.automan.core.logging.tables.DBThunkHistory]
  private val dbQuestion = TableQuery[edu.umass.cs.automan.core.logging.tables.DBQuestion]
  private val dbRadioButtonAnswer = TableQuery[edu.umass.cs.automan.core.logging.tables.DBRadioButtonAnswer]

  // get DB handle
  private val db = Database.forURL(jdbc_conn_string, driver = "scala.slick.driver.DerbyDriver")

  // Thunk existence cache
  private val all_thunk_ids = scala.collection.mutable.Set[UUID]()

  // create DB if it does not already exist
  db.withSession {
    implicit session =>

    if (MTable.getTables("QUESTION").list(session).isEmpty) {
      ( dbThunk.ddl ++
        dbThunkHistory.ddl ++
        dbQuestion.ddl ++
        dbRadioButtonAnswer.ddl
      ).create
    } else {
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
    // TODO: run something like this instead; returns most recent state for each thunk_id, parameterized by memo_hash
    "SELECT history_id,DBThunkHistory.thunk_id,state_change_time,scheduler_state,question_id,cost_in_cents,creation_time,timeout_in_s,worker_timeout_in_s,memo_hash FROM automan.DBThunkHistory INNER JOIN (SELECT thunk_id,MAX(state_change_time) as most_recent FROM automan.DBThunkHistory GROUP BY thunk_id) AS T INNER JOIN DBThunk ON DBThunk.thunk_id = DBThunkHistory.thunk_id INNER JOIN DBQuestion ON DBQuestion.id = DBThunk.question_id WHERE DBThunkHistory.thunk_id = T.thunk_id AND DBThunkHistory.state_change_time = T.most_recent AND memo_hash = 'foo';"
    ???
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
