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
    // TODO: right now, this just returns thunks with actual answer; it really should return everything.
    db.withTransaction {
      implicit session =>
        // get the question entry
        val question: DBQuestion = dbQuestion.filter(_.memo_hash == q.memo_hash).first

        question._3 match {
          case RadioButtonQuestion =>
            val ts_with_answer = for {
              ((thunk, thunkhistory), answer) <- (dbThunk leftJoin dbThunkHistory on (_.thunk_id === _.thunk_id) leftJoin dbRadioButtonAnswer on (_._2.history_id === _.history_id)).run
            } yield (thunk, thunkhistory, answer)
            ts_with_answer.map {
              case (
              (thunk_id, question_id, cost_in_cents, creation_time, timeout_in_s, worker_timeout_in_s),
              (history_id, _, state_change_time, scheduler_state),
              (_, answer, worker_id)
              ) =>
              Thunk(
                thunk_id,
                q,
                timeout_in_s,
                worker_timeout_in_s,
                cost_in_cents,
                creation_time,
                scheduler_state,
                from_memo = true,
                Some(worker_id),
                Some(answer),
                Some(state_change_time)
              ).asInstanceOf[Thunk[A]]
            }.toList
          case _ => ???
        }
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
