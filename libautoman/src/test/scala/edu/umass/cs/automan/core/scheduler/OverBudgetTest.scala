package edu.umass.cs.automan.core.scheduler

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class OverBudgetTest extends FlatSpec with Matchers {
  "A radio button program" should "work" in {
    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.poll_interval = 2
    }

    automan(a) {
      def which_one() = a.RadioButtonQuestion { q =>
        q.budget = 0.00
        q.text = "Which one of these does not belong?"
        q.options = List(
          a.Option('oscar, "Oscar the Grouch"),
          a.Option('kermit, "Kermit the Frog"),
          a.Option('spongebob, "Spongebob Squarepants"),
          a.Option('cookie, "Cookie Monster"),
          a.Option('count, "The Count")
        )
        q.mock_answers = List('spongebob,'spongebob,'spongebob,'spongebob)
      }

      which_one().answer match {
        case Answer(value, cost, conf) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          fail()
        case LowConfidenceAnswer(_, _, _) =>
          fail()
        case OverBudgetAnswer(need, have) =>
          (need > have) should be (true)
      }
    }
  }
}
