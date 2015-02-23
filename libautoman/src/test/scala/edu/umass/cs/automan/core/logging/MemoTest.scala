package edu.umass.cs.automan.core.logging

import edu.umass.cs.automan.adapters.mturk.question.MTRadioButtonQuestion
import org.scalatest._

class MemoTest extends FlatSpec with Matchers {
  val TIMEOUT_IN_S = 600
  val WORKER_TIMEOUT_IN_S = 30
  val BASE_COST = BigDecimal(0.06)

  // init
  // TODO: switch this to MockQuestion later
  val q = new MTRadioButtonQuestion()
  val m = new Memo(LogConfig.TRACE_MEMOIZE_VERBOSE)
  m.wipeDatabase()

  "The Memo class" should "save and restore execution traces when logging is enabled" in {
    val ts = List(
      TestUtil.newThunk(q, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      TestUtil.newThunk(q, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      TestUtil.newThunk(q, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      TestUtil.newThunk(q, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      TestUtil.newThunk(q, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0)
    )

    m.save(q, ts)

    val ts2 = m.restore(q)

    Memo.sameThunks(ts, ts2) should be (true)
  }
}
