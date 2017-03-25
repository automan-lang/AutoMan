package edu.umass.cs.automan.adapter.mturk

import edu.umass.cs.automan.core.logging.LogLevelDebug
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MTurkMultiHITTest extends FlatSpec with Matchers {

  "A radio button program with disagreeing answers" should "spawn multiple HITs." in {
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
      initial_worker_timeout_in_s = 30,
      question_timeout_multiplier = 1,
      text = "Which one of these does not belong?",
      options = List(
        mt.Option('oscar, "Oscar the Grouch"),
        mt.Option('kermit, "Kermit the Frog"),
        mt.Option('spongebob, "Spongebob Squarepants"),
        mt.Option('cookie, "Cookie Monster"),
        mt.Option('count, "The Count")
      ),
      mock_answers =
        makeMocksAt(List('spongebob, 'kermit, 'spongebob), 0) :::
          makeMocksAt(List('spongebob, 'spongebob, 'spongebob), 45000)
    )

    automan(mt, test_mode = true) {
      which_one().answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', confidence: " + conf + ", cost: $" + cost + ", # HITs: " + mt.getAllHITs.length)
          (value == 'spongebob) should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0.48)) should be (true)
          mt.getAllHITs.length should be (2)
        case LowConfidenceAnswer(value, cost, conf, _, _) =>
          fail()
      }
    }
  }
}
