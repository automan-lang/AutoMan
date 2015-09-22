package edu.umass.cs.automan.core.logging

import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class CheckboxDistribMemoTest extends FlatSpec with Matchers {

  "A checkbox distribution program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.TRACE_MEMOIZE_VERBOSE
    }

    // test params
    val sample_size = 30
    val mock_answers = genAnswers(
      Array(Set('spongebob,'count), Set('spongebob), Set('count,'spongebob)),
      Array("0.25", "0.25", "0.50"),
      sample_size
    ).toList

    def which_one(text: String) = a.CheckboxDistributionQuestion { q =>
      q.sample_size = sample_size
      q.budget = 8.00
      q.text = text
      q.options = List(
        a.Option('oscar, "Oscar the Grouch"),
        a.Option('kermit, "Kermit the Frog"),
        a.Option('spongebob, "Spongebob Squarepants"),
        a.Option('cookie, "Cookie Monster"),
        a.Option('count, "The Count")
      )
      q.mock_answers = makeMocksAt(mock_answers.toList, 0)
    }

    def which_one2(text: String) = a.CheckboxDistributionQuestion { q =>
      q.sample_size = sample_size
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

    automan(a, test_mode = true) {
      which_one("Which one of these does not belong?").answer match {
        case Answers(values, cost) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          compareDistributions(mock_answers, values) should be (true)
          cost should be (BigDecimal(0.06) * sample_size)
        case _ =>
          fail()
      }

      which_one2("Which one of these does not belong?").answer match {
        case Answers(values, cost) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          compareDistributions(mock_answers, values) should be (true)
          (cost == BigDecimal(0.00)) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
