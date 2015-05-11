package edu.umass.cs.automan.adapter.mturk

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.MTurkAdapter
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.logging.LogConfig
import org.scalatest._

class MTurkRadioTest extends FlatSpec with Matchers {

  "A radio button program" should "work" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.poll_interval = 2
    }

    automan(a) {
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
        q.mock_answers = List('spongebob,'spongebob,'spongebob,'spongebob,'spongebob,'spongebob)
      }

      which_one().answer match {
        case Answer(value, _, conf) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == 'spongebob) should be (true)
          (conf >= confidence) should be (true)
        case LowConfidenceAnswer(value, cost, conf) =>
          fail()
      }
    }
  }
}
