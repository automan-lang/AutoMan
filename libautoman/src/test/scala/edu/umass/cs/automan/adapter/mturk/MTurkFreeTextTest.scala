package edu.umass.cs.automan.adapter.mturk

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.MTurkAdapter
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.logging.LogConfig
import org.scalatest._

class MTurkFreeTextTest extends FlatSpec with Matchers {

  "A freetext program" should "work" in {
    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.poll_interval = 2
    }

    automan(a) {
      def which_one() = a.FreeTextQuestion { q =>
        q.budget = 8.00
        q.text = "Which 4-letter metasyntactic variable starts with 'q'?"
        q.pattern = "AAAA"
        q.mock_answers = List("quux","foo","bar","norf","quux","quux")
      }

      which_one().answer match {
        case ScalarAnswer(value, _, _) =>
          (value == "quux") should be (true)
        case ScalarOverBudget(value, cost, conf) =>
          fail()
      }
    }
  }
}
