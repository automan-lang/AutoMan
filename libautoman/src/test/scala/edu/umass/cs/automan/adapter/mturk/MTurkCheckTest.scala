package edu.umass.cs.automan.adapter.mturk

import edu.umass.cs.automan.core.logging.LogLevelDebug
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MTurkCheckTest extends FlatSpec with Matchers {

  "A checkbox program" should "work" in {
    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(budget = 8.00),
      logging = LogConfig.NO_LOGGING,
      log_verbosity = LogLevelDebug()
    )

    automan(mt, test_mode = true) {
      def which_ones() = checkbox (
        confidence = confidence,
        budget = 8.00,
        text = "Which characters are not Oscar, Kermit, or Cookie Monster?",
        options = List(
          mt.Option('oscar, "Oscar the Grouch"),
          mt.Option('kermit, "Kermit the Frog"),
          mt.Option('spongebob, "Spongebob Squarepants"),
          mt.Option('cookie, "Cookie Monster"),
          mt.Option('count, "The Count")
        ),
        // temporary hack to deal with MTurk spawn minimums
        mock_answers = makeMocks(
          List(
            Set('spongebob,'count),
            Set('spongebob),
            Set('count,'spongebob),
            Set('count,'spongebob),
            Set('count,'spongebob),
            Set('count,'spongebob),
            Set('count,'spongebob),
            Set('count,'spongebob),
            Set('count,'spongebob),
            Set('count,'spongebob)
          )
        )
      )

      which_ones().answer match {
        case Answer(value, _, conf, _, _) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
