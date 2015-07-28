package edu.umass.cs.automan.core.logging

import edu.umass.cs.automan.adapters.mturk.question.MTRadioButtonQuestion
import edu.umass.cs.automan.test._
import org.scalatest._

class MemoTest extends FlatSpec with Matchers {
  val TIMEOUT_IN_S = 600
  val WORKER_TIMEOUT_IN_S = 30
  val BASE_COST = BigDecimal(0.06)

  // init
  // TODO: switch this to MockQuestion later
  val q = new MTRadioButtonQuestion()
  val m = new Memo(LogConfig.TRACE_MEMOIZE_VERBOSE)

  "The Memo class" should "save and restore execution traces when logging is enabled" in {
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
  }

  "The Memo class" should "save and restore complex execution traces when logging is enabled" in {
    // clear state
    m.wipeDatabase()

    val ts = List(
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0)
    )

    m.save(q, ts)

    val ts2 = ts.map(_.copy_as_running())

    m.save(q, ts2)

    val t = ts2(1)

    val ts3 = List(
      ts2(0),
      ts2(1).copy_with_answer('yes.asInstanceOf[t.question.A], "ABCD1234"),
      ts2(2).copy_with_answer('no.asInstanceOf[t.question.A], "DCBA4321"),
      ts2(3).copy_as_timeout(),
      ts2(4).copy_as_cancelled(),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0),
      newTask(q, 0, TIMEOUT_IN_S, WORKER_TIMEOUT_IN_S, BASE_COST, 0)
    )

    m.save(q, ts3)

    val ts4 = m.restore(q)

    Memo.sameTasks(ts3, ts4) should be (true)
  }
}
