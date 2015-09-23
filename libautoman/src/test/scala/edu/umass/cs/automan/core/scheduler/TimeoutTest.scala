package edu.umass.cs.automan.core.scheduler

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class TimeoutTest extends FlatSpec with Matchers {
  "A radio button program" should "timeout and double reward" in {
    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
    }

    automan(a) {
      def which_one() = a.RadioButtonQuestion { q =>
        q.budget = 0.30
        q.text = "Which one of these does not belong?"
        // make sure that this task times out after exactly 30s
        q.initial_worker_timeout_in_s = 30
        q.question_timeout_multiplier = 1
        q.options = List(
          a.Option('oscar, "Oscar the Grouch"),
          a.Option('kermit, "Kermit the Frog"),
          a.Option('spongebob, "Spongebob Squarepants"),
          a.Option('cookie, "Cookie Monster"),
          a.Option('count, "The Count")
        )
        q.mock_answers = makeTimedMocks(
          List(
            ('spongebob, 0),
            ('spongebob, 45000),
            ('spongebob, 45000)
          )
        )
      }

      which_one().answer match {
        case Answer(value, cost, conf) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == 'spongebob) should be (true)
          cost should be (BigDecimal(0.24))
          (conf > 0.95) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
