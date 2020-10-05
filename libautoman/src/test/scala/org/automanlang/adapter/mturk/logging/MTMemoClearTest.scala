package org.automanlang.adapter.mturk.logging

import java.util.UUID
import org.automanlang.adapters.mturk.logging.MTMemo
import org.automanlang.adapters.mturk.question.MTRadioButtonQuestion
import org.automanlang.core.logging.{LogConfig, Memo}
import org.automanlang.test._
import org.scalatest._

class MTMemoClearTest extends FlatSpec with Matchers {
  "The MTMemo class" should "clear its contents when clearDatabase is called" in {
    val TIMEOUT_IN_S = 600
    val WORKER_TIMEOUT_IN_S = 30
    val BASE_COST = BigDecimal(0.06)
    val DB = "MTMemoClearTest_" + UUID.randomUUID()

    // init
    val q = new MTRadioButtonQuestion()
    val m = new MTMemo(LogConfig.TRACE_MEMOIZE_VERBOSE, DB, false)

    // create database
    m.init()

    val ts = List(
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0)
    )

    m.save(q, ts, List.empty)

    val ts2 = m.restore(q)

    Memo.sameTasks(ts, ts2) should be (true)

    m.wipeDatabase()

    // we expect an exception here
    intercept[java.sql.SQLException] { m.restore(q) }
  }
}
