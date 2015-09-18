package edu.umass.cs.automan.adapter.mturk.logging

import edu.umass.cs.automan.adapters.mturk.logging.MTMemo
import edu.umass.cs.automan.adapters.mturk.question.MTRadioButtonQuestion
import edu.umass.cs.automan.core.logging.{LogConfig, Memo}
import edu.umass.cs.automan.test._
import org.scalatest._

class MTMemoClearTest extends FlatSpec with Matchers {
  "The MTMemo class" should "clear its contents when clearDatabase is called" in {
    val TIMEOUT_IN_S = 600
    val WORKER_TIMEOUT_IN_S = 30
    val BASE_COST = BigDecimal(0.06)

    // init
    val q = new MTRadioButtonQuestion()
    val m = new MTMemo(LogConfig.TRACE_MEMOIZE_VERBOSE)

    // clear state
    m.wipeDatabase()
    // recreate database
    m.init()

    val ts = List(
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0)
    )

    m.save(q, ts)

    val ts2 = m.restore(q)

    Memo.sameTasks(ts, ts2) should be (true)

    m.wipeDatabase()

    val ts3 = m.restore(q)

    Memo.sameTasks(ts, ts3) should be (false)
  }
}
