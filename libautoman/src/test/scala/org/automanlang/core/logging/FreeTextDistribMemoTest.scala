package org.automanlang.core.logging

import org.scalatest._
import java.util.UUID
import org.automanlang.test._
import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.mock.MockSetup

class FreeTextDistribMemoTest extends FlatSpec with Matchers {

  "A freetext distribution program" should "correctly recall answers at no cost" in {
    val confidence = 0.95

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      log_verbosity = LogLevelDebug()
    )

    // test params
    val sample_size = 30
    val mock_answers = genAnswers(
      Array("oscar", "kermit", "spongebob", "cookie", "count"),
      Array("0.02", "0.14", "0.78", "0.05", "0.01"),
      sample_size
    ).toList

    def which_one(text: String) = freetexts (
      sample_size = sample_size,
      budget = 8.00,
      text = text,
      mock_answers = makeMocksAt(mock_answers.toList, 0)
    )

    def which_one2(text: String) = mt.FreeTextDistributionQuestion { q =>
      q.sample_size = sample_size
      q.budget = 8.00
      q.text = text
      q.mock_answers = List()
    }

    automan(mt, test_mode = true, in_mem_db = true) {
      which_one("Which one of these does not belong?").answer match {
        case Answers(values, cost, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          compareDistributions(mock_answers, values) should be (true)
          cost should be (BigDecimal(0.06) * sample_size)
        case _ =>
          fail()
      }

      which_one2("Which one of these does not belong?").answer match {
        case Answers(values, cost, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "'")
          compareDistributions(mock_answers, values) should be (true)
          (cost == BigDecimal(0.00)) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
