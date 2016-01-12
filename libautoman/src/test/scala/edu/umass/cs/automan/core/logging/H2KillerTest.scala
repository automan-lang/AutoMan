package edu.umass.cs.automan.core.logging

import java.util.UUID
import edu.umass.cs.automan.core.answer.OverBudgetEstimate

import scala.util.Random
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import org.scalatest.{Matchers, FlatSpec}

class H2KillerTest extends FlatSpec with Matchers {
  "An estimator program" should "not crash under high load" in {
    val dollar_budget = BigDecimal(8.00)
    val wage = BigDecimal(7.25)
    val time_budget_in_sec = 30  // in seconds
    val hour_in_sec = 3600
    val reward = (wage / (hour_in_sec / time_budget_in_sec)).setScale(2, BigDecimal.RoundingMode.HALF_DOWN)
    val n = (dollar_budget / reward).setScale(0, BigDecimal.RoundingMode.FLOOR) + 1
    val rng = new Random()

    // this set of values is virtually guaranteed not
    // to have a CI of +/- 50 which means that it should
    // exceed the budget first
    val mock_answers = Seq.fill(n.toInt)(rng.nextDouble() * 100000)

    def getMockSeq = makeMocks[Double](rng.shuffle(mock_answers).toList)

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.TRACE_MEMOIZE_VERBOSE
      mt.database_path = "H2KillerTestDB"
    }

    def countJellies() = a.EstimationQuestion { q =>
      q.confidence = 0.95
      q.budget = dollar_budget
      q.wage = wage
      q.text = "How many jelly beans are in this jar?"
      q.confidence_interval = SymmetricCI(50)
      q.mock_answers = getMockSeq
    }

    try {
      val answers = (0 until 15).map { i => countJellies() }

      val outcome = answers.map { a =>
        a.answer match {
          case OverBudgetEstimate(_, _) => true
          case _ => false
        }
      }.foldLeft(true) { case (acc, v) => acc && v }

      // all estimates should be over budget.
      // if anything else happens, the test failed
      outcome should be(true)
    } catch {
      case _: Throwable => fail()
    }

    // cleanup
    a.memo_delete()
  }
}
