package edu.umass.cs.automan.adapter.mturk

import edu.umass.cs.automan.core.logging.{DebugLog, LogLevelDebug}
import org.scalatest._
import java.util.UUID

import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MTurkDisposeQualsTest extends FlatSpec with Matchers {

  "An AutoMan program" should "dispose all of its qualifications on shutdown" in {
    val confidence = 0.95

    var qualIDs = List.empty[String]
    var wasNonempty = false

    val create_callback = (msg: String) => {
      val regex = """Creating disqualification ID: ([0-9a-z-]{36}).""".r
      val matcher = regex.pattern.matcher(msg)
      if (matcher.find()) {
        qualIDs = matcher.group(1) :: qualIDs
        wasNonempty = true
      }
    }

    val dispose_callback = (msg: String) => {
      val regex = """Deleting disqualification ID: ([0-9a-z-]{36}).""".r
      val matcher = regex.pattern.matcher(msg)
      if (matcher.find()) {
        qualIDs = qualIDs.filter(_ != matcher.group(1))
      }
    }

    DebugLog.subscribeCallback(create_callback)
    DebugLog.subscribeCallback(dispose_callback)

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.log_verbosity = LogLevelDebug()
    }

    automan(a, test_mode = true) {
      def which_ones() = a.CheckboxQuestion { q =>
        q.confidence = confidence
        q.budget = 8.00
        q.text = "Which characters are not Oscar, Kermit, or Cookie Monster?"
        q.options = List(
          a.Option('oscar, "Oscar the Grouch"),
          a.Option('kermit, "Kermit the Frog"),
          a.Option('spongebob, "Spongebob Squarepants"),
          a.Option('cookie, "Cookie Monster"),
          a.Option('count, "The Count")
        )
        // temporary hack to deal with MTurk spawn minimums
        q.mock_answers = makeMocks(
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
      }

      which_ones().answer match {
        case Answer(value, _, conf, _, _) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
        case _ =>
          fail()
      }
    }

    wasNonempty should be (true)
    qualIDs.size should be (0)
  }
}
