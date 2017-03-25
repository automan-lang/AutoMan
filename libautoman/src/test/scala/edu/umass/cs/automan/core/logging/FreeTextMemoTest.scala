package edu.umass.cs.automan.core.logging

import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class FreeTextMemoTest extends FlatSpec with Matchers {

  "A freetext program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(budget = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      log_verbosity = LogLevelDebug()
    )

    def which_one(text: String) = freetext (
      confidence = 0.95,
      budget = 8.00,
      text = text,
      pattern = "AAAA",
      mock_answers = makeMocksAt(List("quux","foo","bar","norf","quux","quux"), 0),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    def which_one2(text: String) = freetext (
      confidence = 0.95,
      budget = 8.00,
      text = text,
      pattern = "AAAA",
      mock_answers = List(),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    automan(mt, test_mode = true, in_mem_db = true) {
      which_one("Which 4-letter metasyntactic variable starts with 'q'?").answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == "quux") should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0.12)) should be(true)
        case _ =>
          fail()
      }

      which_one2("Which 4-letter metasyntactic variable starts with 'q'?").answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == "quux") should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0)) should be(true)
        case _ =>
          fail()
      }
    }
  }
}
