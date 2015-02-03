package edu.umass.cs.automan.core.logging

import java.util.UUID
import edu.umass.cs.automan.core.scheduler.SchedulerState
import scala.slick.driver.DerbyDriver.simple._
import scala.slick.jdbc.meta.MTable

class Memo(log_config: LogConfig.Value) {
  // connection string
  private val jdbc_conn_string = "jdbc:derby:AutoManMemoDB"

  // tables
  private val thunk = TableQuery[edu.umass.cs.automan.core.logging.tables.Thunk]
  private val thunk_history = TableQuery[edu.umass.cs.automan.core.logging.tables.ThunkHistory]
  private val question = TableQuery[edu.umass.cs.automan.core.logging.tables.Question]
  private val question_thunk = TableQuery[edu.umass.cs.automan.core.logging.tables.QuestionThunk]
  private val radio_button_answer = TableQuery[edu.umass.cs.automan.core.logging.tables.RadioButtonAnswer]

  // get DB handle
  private val db = Database.forURL(jdbc_conn_string, driver = "scala.slick.driver.DerbyDriver")

  // Thunk existence cache
  private val all_thunk_ids = scala.collection.mutable.Set[UUID]()

  // create DB if it does not already exist
  db.withSession {
    implicit session =>

    if (MTable.getTables("QUESTION").list(session).isEmpty) {
      ( thunk.ddl ++
        thunk_history.ddl ++
        question.ddl ++
        question_thunk.ddl ++
        radio_button_answer.ddl
      ).create
    } else {
      // add all of the thunk_ids from the database to the tracked set
      thunk.map(_.thunk_id).foreach(all_thunk_ids.add(_))
    }
  }

  def saveThunk(t: edu.umass.cs.automan.core.scheduler.Thunk[_]) : Unit = {
    // if the Thunk already exists, just update the ThunkHistory table
    if (all_thunk_ids.contains(t.thunk_id)) {
      assert(t.state != SchedulerState.READY)

      db.withTransaction {
        implicit session =>

          thunk_history += (t.thunk_id, t.created_at, t.state, t.worker_id)
      }
    } else {
      assert(t.state == SchedulerState.READY)

      // otherwise, create a Thunk entry
      db.withTransaction {
        implicit session =>

          thunk += (t.thunk_id, t.computation_id, t.cost, t.created_at, t.question.id, t.timeout_in_s, t.worker_timeout)
          thunk_history += (t.thunk_id, t.created_at, t.state, t.worker_id)
      }
      all_thunk_ids.add(t.thunk_id)
    }
  }
}
