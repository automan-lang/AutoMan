package org.automanlang.adapter.mturk

import org.automanlang.core.logging.{DebugLog, LogLevelDebug}
import org.scalatest._
import java.util.UUID

import org.automanlang.test._
import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.mock.MockSetup

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

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.NO_LOGGING,
      log_verbosity = LogLevelDebug()
    )

    automan(mt, test_mode = true) {
      def which_ones() = checkbox(
        confidence = confidence,
        budget = 8.00,
        text = "Which characters are not Oscar, Kermit, or Cookie Monster?",
        options = List(
          choice('oscar, "Oscar the Grouch"),
          choice('kermit, "Kermit the Frog"),
          choice('spongebob, "Spongebob Squarepants"),
          choice('cookie, "Cookie Monster"),
          choice('count, "The Count")
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

    wasNonempty should be (true)
    qualIDs.size should be (0)
  }
}
