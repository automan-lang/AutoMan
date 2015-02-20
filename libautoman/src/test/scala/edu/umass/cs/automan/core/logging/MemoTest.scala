package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.mturk.question.MTRadioButtonQuestion
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import org.scalatest._

object TestUtil {
  def newThunk[A](question: Question[A], timeout: Int, worker_timeout: Int, cost: BigDecimal, time_delta: Int) =
    Thunk[A](
      UUID.randomUUID(),
      question,
      timeout,
      worker_timeout,
      cost,
      new Date(),
      SchedulerState.READY,
      from_memo = false, None, None, None
    )
}

class MemoTest extends FlatSpec with Matchers {
  val TIMEOUT_IN_S = 600
  val WORKER_TIMEOUT_IN_S = 30
  val BASE_COST = BigDecimal(0.06)

  "The Memo class" should "save and restore execution traces when logging is enabled" in {
    // init
    // TODO: switch this to Mock later
    val q = new MTRadioButtonQuestion()
    val m = new Memo(LogConfig.TRACE_MEMOIZE_VERBOSE)

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
