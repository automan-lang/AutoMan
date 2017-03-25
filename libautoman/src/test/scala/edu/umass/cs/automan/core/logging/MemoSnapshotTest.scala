package edu.umass.cs.automan.core.logging

import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MemoSnapshotTest extends FlatSpec with Matchers {

  "The memoizer snapshot call" should "return all tasks" in {
    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(budget = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      log_verbosity = LogLevelDebug()
    )

    def which_one() = radio (
      confidence = confidence,
      budget = 8.00,
      text = "Which one of these does not belong?",
      options = List(
        choice('oscar, "Oscar the Grouch"),
        choice('kermit, "Kermit the Frog"),
        choice('spongebob, "Spongebob Squarepants"),
        choice('cookie, "Cookie Monster"),
        choice('count, "The Count")
      ),
      mock_answers = makeMocksAt(List('spongebob,'spongebob,'spongebob,'spongebob,'spongebob,'spongebob), 0),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    def which_one2(text: String) = checkbox (
      confidence = confidence,
      budget = 8.00,
      text = text,
      options = List(
        choice('oscar, "Oscar the Grouch"),
        choice('kermit, "Kermit the Frog"),
        choice('spongebob, "Spongebob Squarepants"),
        choice('cookie, "Cookie Monster"),
        choice('count, "The Count")
      ),
      mock_answers = makeMocksAt(List(Set('spongebob,'count),Set('spongebob),Set('count,'spongebob),Set('count,'spongebob)), 0),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    automan(mt, test_mode = true, in_mem_db = true) {
      which_one().answer match {
        case Answer(value, _, conf, _, _) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == 'spongebob) should be (true)
          (conf >= confidence) should be (true)
        case LowConfidenceAnswer(value, cost, conf, _, _) =>
          fail()
      }

      // NOTE: ranges are given here for tests because of numerical
      //       instability in the monte carlo simulations, despite
      //       having a precomputed table, because AutoMan always
      //       reruns the simulation to compute the empirical confidence
      //       and sometimes the empirical confidence varies enough
      //       to spawn more tasks.

      which_one2("Which characters are not Oscar, Kermit, or Cookie Monster?").answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
          (cost >= BigDecimal(2) * BigDecimal(0.06) && cost
            <= BigDecimal(3) * BigDecimal(0.06)) should be(true)
        case _ =>
          fail()
      }

      val snap = mt.state_snapshot()
      snap.size >= 5 && snap.size <= 7 should be (true)
    }
  }
}
