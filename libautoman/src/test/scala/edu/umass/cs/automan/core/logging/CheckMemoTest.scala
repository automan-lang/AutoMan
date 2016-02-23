package edu.umass.cs.automan.core.logging

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class CheckMemoTest extends FlatSpec with Matchers {

  "A checkbox program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.TRACE_MEMOIZE_VERBOSE
      mt.log_verbosity = LogLevelDebug()
    }

    def which_one(text: String) = a.CheckboxQuestion { q =>
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
      q.mock_answers = List()
    }

    automan(a, test_mode = true, in_mem_db = true) {
      which_one("Which characters are not Oscar, Kermit, or Cookie Monster?").answer match {
        case Answer(value, cost, conf, _) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
          (cost >= BigDecimal(2) * BigDecimal(0.06) &&
            cost <= BigDecimal(3) * BigDecimal(0.06)) should be(true)
        case _ =>
          fail()
      }

      which_one2("Which characters are not Oscar, Kermit, or Cookie Monster?").answer match {
        case Answer(value, cost, conf, _) =>
          println("Answer: '" + value + "', confidence: " + conf)
          (value == Set('spongebob,'count)) should be (true)
          (conf >= confidence) should be (true)
          (cost == BigDecimal(0)) should be(true)
        case _ =>
          fail()
      }
    }
  }
}
