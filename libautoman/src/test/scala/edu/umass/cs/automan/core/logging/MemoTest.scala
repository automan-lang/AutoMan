package edu.umass.cs.automan.core.logging

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.question.MTRadioButtonQuestion
import edu.umass.cs.automan.test._
import org.scalatest._

class MemoTest extends FlatSpec with Matchers {
  val TIMEOUT_IN_S = 600
  val WORKER_TIMEOUT_IN_S = 30
  val BASE_COST = BigDecimal(0.06)
  val DB = "MemoTest_" + UUID.randomUUID()

  // init
  val q = new MTRadioButtonQuestion()
  val m = new Memo(LogConfig.TRACE_MEMOIZE_VERBOSE, DB, false)

  "The Memo class" should "save and restore execution traces when logging is enabled" in {
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

    // cleanup
    m.wipeDatabase()
  }

  "The Memo class" should "save and restore complex execution traces when logging is enabled" in {
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

    val ts2 = ts.map(_.copy_as_running())

    m.save(q, List.empty, ts2)

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

    m.save(q, ts3.slice(5,7), ts3.slice(1,5))

    val ts4 = m.restore(q)

    Memo.sameTasks(ts3, ts4) should be (true)

    // cleanup
    m.wipeDatabase()
  }
}
