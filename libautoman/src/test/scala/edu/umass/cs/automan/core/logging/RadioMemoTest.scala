package edu.umass.cs.automan.core.logging

import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class RadioMemoTest extends FlatSpec with Matchers {

  "A radio button program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      log_verbosity = LogLevelDebug()
    )

    def which_one(text: String) = radio (
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
      mock_answers = makeMocksAt(List('spongebob,'spongebob,'spongebob,'spongebob,'spongebob,'spongebob), 0),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    def which_one2(text: String) = radio (
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
      mock_answers = List(),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    automan(mt, test_mode = true, in_mem_db = true) {
      which_one("Which one of these does not belong?").answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == 'spongebob) should be(true)
          (conf >= confidence) should be(true)
          (cost == BigDecimal(3) * BigDecimal(0.06)) should be(true)
        case _ =>
          fail()
      }

      which_one2("Which one of these does not belong?").answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == 'spongebob) should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0.00)) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
