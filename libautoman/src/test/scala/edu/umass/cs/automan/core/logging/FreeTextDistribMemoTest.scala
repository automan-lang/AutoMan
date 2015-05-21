package edu.umass.cs.automan.core.logging

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.MTurkAdapter
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import edu.umass.cs.automan.{TestUtil, automan}
import edu.umass.cs.automan.core.answer._
import org.scalatest._

class FreeTextDistribMemoTest extends FlatSpec with Matchers {

  "A freetext distribution program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.TRACE_MEMOIZE_VERBOSE
      mt.poll_interval = 2
    }

    // clear, just to be safe
    a.clearMemoDB()

    // test params
    val sample_size = 30
    var reward = BigDecimal(0)
    val mock_answers = TestUtil.genAnswers(
      Array("oscar", "kermit", "spongebob", "cookie", "count"),
      Array("0.02", "0.14", "0.78", "0.05", "0.01"),
      sample_size
    ).toList

    automan(a) {
      def which_one(text: String) = a.FreeTextDistributionQuestion { q =>
        q.sample_size = sample_size
        q.budget = 8.00
        q.text = text
        q.mock_answers = mock_answers.toList
        reward = q.reward
      }

      def which_one2(text: String) = a.FreeTextDistributionQuestion { q =>
        q.sample_size = sample_size
        q.budget = 8.00
        q.text = text
        q.mock_answers = List()
      }

      which_one("Which one of these does not belong?").answer match {
        case Answers(values, cost) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          TestUtil.compareDistributions(mock_answers, values) should be (true)
          cost should be (reward * sample_size)
        case _ =>
          fail()
      }

      which_one2("Which one of these does not belong?").answer match {
        case Answers(values, cost) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          TestUtil.compareDistributions(mock_answers, values) should be (true)
          (cost == BigDecimal(0.00)) should be (true)
        case _ =>
          fail()
      }
    }

    // clear, just to be a nice guy
    a.clearMemoDB()
  }
}
