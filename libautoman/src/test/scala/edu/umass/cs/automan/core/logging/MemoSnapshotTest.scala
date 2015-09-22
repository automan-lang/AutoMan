package edu.umass.cs.automan.core.logging

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class MemoSnapshotTest extends FlatSpec with Matchers {

  "The memoizer snapshot call" should "return all tasks" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.TRACE_MEMOIZE_VERBOSE
    }

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
      q.mock_answers = makeMocksAt(List('spongebob,'spongebob,'spongebob,'spongebob,'spongebob,'spongebob), 0)
    }

    def which_one2(text: String) = a.CheckboxQuestion { q =>
      q.confidence = confidence
      q.budget = 8.00
      q.text = text
      q.options = List(
        a.Option('oscar, "Oscar the Grouch"),
        a.Option('kermit, "Kermit the Frog"),
        a.Option('spongebob, "Spongebob Squarepants"),
        a.Option('cookie, "Cookie Monster"),
        a.Option('count, "The Count")
      )
      q.mock_answers = makeMocksAt(List(Set('spongebob,'count),Set('spongebob),Set('count,'spongebob),Set('count,'spongebob)), 0)
    }

    automan(a, test_mode = true) {
      which_one().answer match {
        case Answer(value, _, conf) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == 'spongebob) should be (true)
          (conf >= confidence) should be (true)
        case LowConfidenceAnswer(value, cost, conf) =>
          fail()
      }

      which_one2("Which characters are not Oscar, Kermit, or Cookie Monster?").answer match {
        case Answer(value, cost, conf) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(3) * BigDecimal(0.06)) should be(true)
        case _ =>
          fail()
      }

      a.state_snapshot().size should be (7)
    }
  }
}
