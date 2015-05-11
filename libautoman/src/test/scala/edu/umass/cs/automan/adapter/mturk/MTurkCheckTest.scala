package edu.umass.cs.automan.adapter.mturk

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.MTurkAdapter
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.logging.LogConfig
import org.scalatest._

class MTurkCheckTest extends FlatSpec with Matchers {

  "A checkbox program" should "work" in {
    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.poll_interval = 2
    }

    automan(a) {
      def which_one() = a.CheckboxQuestion { q =>
        q.budget = 8.00
        q.text = "Which characters are not Oscar, Kermit, or Cookie Monster?"
        q.options = List(
          a.Option('oscar, "Oscar the Grouch"),
          a.Option('kermit, "Kermit the Frog"),
          a.Option('spongebob, "Spongebob Squarepants"),
          a.Option('cookie, "Cookie Monster"),
          a.Option('count, "The Count")
        )
        q.mock_answers = List(Set('spongebob,'count),Set('spongebob),Set('count,'spongebob),Set('count,'spongebob))
      }

      which_one().answer match {
        case Answer(value, _, _) =>
          (value == Set('spongebob,'count)) should be (true)
        case LowConfidenceAnswer(value, cost, conf) =>
          fail()
      }
    }
  }
}
