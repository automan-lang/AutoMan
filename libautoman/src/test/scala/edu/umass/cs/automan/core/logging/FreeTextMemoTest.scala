package edu.umass.cs.automan.core.logging

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.MTurkAdapter
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer._
import org.scalatest._

class FreeTextMemoTest extends FlatSpec with Matchers {

  "A freetext program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.TRACE_MEMOIZE_VERBOSE
      mt.poll_interval = 2
    }

    // clear, just to be safe
    a.clearMemoDB()

    automan(a) {
      def which_one(text: String) = a.FreeTextQuestion { q =>
        q.confidence = 0.95
        q.budget = 8.00
        q.text = text
        q.pattern = "AAAA"
        q.mock_answers = List("quux","foo","bar","norf","quux","quux")
      }

      def which_one2(text: String) = a.FreeTextQuestion { q =>
        q.confidence = 0.95
        q.budget = 8.00
        q.text = text
        q.pattern = "AAAA"
        q.mock_answers = List()
      }

      which_one("Which 4-letter metasyntactic variable starts with 'q'?").answer match {
        case Answer(value, cost, conf) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == "quux") should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0.12)) should be(true)
        case _ =>
          fail()
      }

      which_one2("Which 4-letter metasyntactic variable starts with 'q'?").answer match {
        case Answer(value, cost, conf) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == "quux") should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0)) should be(true)
        case _ =>
          fail()
      }
    }

    // clear, just to be a nice guy
    a.clearMemoDB()
  }
}
