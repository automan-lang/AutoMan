package edu.umass.cs.automan.adapter.mturk

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MTurkFreeTextTest extends FlatSpec with Matchers {

  "A freetext program" should "work" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.poll_interval = 2
    }

    automan(a, test_mode = true) {
      def which_one() = a.FreeTextQuestion { q =>
        q.confidence = 0.95
        q.budget = 8.00
        q.text = "Which 4-letter metasyntactic variable starts with 'q'?"
        q.pattern = "AAAA"
        q.mock_answers = makeMocks(List("quux","foo","bar","norf","quux","quux"))
      }

      which_one().answer match {
        case Answer(value, _, conf) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == "quux") should be (true)
          (conf >= confidence) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
