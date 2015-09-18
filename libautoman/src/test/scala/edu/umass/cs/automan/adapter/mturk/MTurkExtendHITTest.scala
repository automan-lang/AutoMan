package edu.umass.cs.automan.adapter.mturk

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MTurkExtendHITTest extends FlatSpec with Matchers {

  "A radio button program" should "work" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.poll_interval = 2
    }

    def which_one() = a.RadioButtonQuestion { q =>
      q.confidence = confidence
      q.budget = 8.00
      q.text = "Which one of these does not belong?"
      q.options = List(
        a.Option('oscar, "Oscar the Grouch"),
        a.Option('kermit, "Kermit the Frog"),
        a.Option('spongebob, "Spongebob Squarepants"),
        a.Option('cookie, "Cookie Monster"),
        a.Option('count, "The Count")
      )
      q.mock_answers = makeMocks(
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
        'spongebob
      )
    }

    automan(a, test_mode = true) {
      which_one().answer match {
        case Answer(value, cost, conf) =>
          println("Answer: '" + value + "', confidence: " + conf + ", cost: $" + cost + ", # HITs: " + a.getAllHITs.length)
          (value == 'spongebob) should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0.42)) should be (true)
          a.getAllHITs.length should be (1)
        case LowConfidenceAnswer(value, cost, conf) =>
          fail()
      }
    }
  }
}
