package edu.umass.cs.automan.adapter.mturk

import edu.umass.cs.automan.core.logging.LogLevelDebug
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MTurkExtendHITTest extends FlatSpec with Matchers {

  "A radio button program" should "work" in {
    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(budget = 8.00),
      logging = LogConfig.NO_LOGGING,
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
      mock_answers = makeMocks(
        'spongebob,
        'kermit,
        'spongebob,
        'kermit,
        'spongebob,
        'kermit,
        'spongebob,
        'kermit,
        'spongebob,
        'spongebob,
        'spongebob,
        'spongebob
      ),
      // opt out of MTurk spawn min to simplify test
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    automan(mt, test_mode = true) {
      which_one().answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', confidence: " + conf + ", cost: $" + cost + ", # HITs: " + mt.getAllHITs.length)
          (value == 'spongebob) should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0.06) * BigDecimal(7)) should be (true)
          mt.getAllHITs.length should be (1)
        case LowConfidenceAnswer(value, cost, conf, _, _) =>
          fail()
      }
    }
  }
}
