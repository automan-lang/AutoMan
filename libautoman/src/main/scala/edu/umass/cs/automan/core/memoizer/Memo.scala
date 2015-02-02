package edu.umass.cs.automan.core.memoizer

import java.util.Date

import edu.umass.cs.automan.core.scheduler.SchedulerState

import scala.slick.driver.DerbyDriver.simple._
import scala.slick.jdbc.meta.MTable

class Memo {
  // connection string
  private val jdbc_conn_string = "jdbc:derby:AutoManMemoDB"

  // tables
  private val thunk = TableQuery[edu.umass.cs.automan.core.memoizer.tables.Thunk]
  private val thunk_history = TableQuery[edu.umass.cs.automan.core.memoizer.tables.ThunkHistory]
  private val question = TableQuery[edu.umass.cs.automan.core.memoizer.tables.Question]
  private val question_thunk = TableQuery[edu.umass.cs.automan.core.memoizer.tables.QuestionThunk]
  private val radio_button_answer = TableQuery[edu.umass.cs.automan.core.memoizer.tables.RadioButtonAnswer]

  // get DB handle
  private val db = Database.forURL(jdbc_conn_string, driver = "scala.slick.driver.DerbyDriver")

  db.withSession {
    implicit session =>

    if (MTable.getTables("QUESTION").list(session).isEmpty) {
      ( thunk.ddl ++
        thunk_history.ddl ++
        question.ddl ++
        question_thunk.ddl ++
        radio_button_answer.ddl
      ).create
    }
  }

  def saveNewThunk(t: edu.umass.cs.automan.core.scheduler.Thunk[_]) : Unit = {
    db.withTransaction {
      implicit session =>
        assert(t.state == SchedulerState.READY)

        thunk += (t.thunk_id, t.computation_id, t.cost, t.created_at, t.question.id, t.timeout_in_s, t.worker_timeout)
        thunk_history += (t.thunk_id, t.created_at, t.state, t.worker_id)
    }
  }

  def updateThunk(t: edu.umass.cs.automan.core.scheduler.Thunk[_]) : Unit = {
    db.withTransaction {
      implicit session =>
        assert(t.state != SchedulerState.READY)

        thunk_history += (t.thunk_id, new Date(), t.state, t.worker_id)
    }
  }
}
