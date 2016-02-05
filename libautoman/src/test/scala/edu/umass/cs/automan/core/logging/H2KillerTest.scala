package edu.umass.cs.automan.core.logging

import java.util.UUID
import edu.umass.cs.automan.core.answer.{LowConfidenceEstimate, OverBudgetEstimate}
import edu.umass.cs.automan.core.mock.MockAnswer
import scala.util.Random
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import org.scalatest.{Matchers, FlatSpec}

class H2KillerTest extends FlatSpec with Matchers {
  "An estimator program" should "not experience a database primary key error under load" in {
    val dollar_budget = BigDecimal(8.00)
    val wage = BigDecimal(7.25)
    val time_budget_in_sec = 30  // in seconds
    val hour_in_sec = 3600
    val reward = (wage / (hour_in_sec / time_budget_in_sec)).setScale(2, BigDecimal.RoundingMode.HALF_DOWN)
    val n = (dollar_budget / reward).setScale(0, BigDecimal.RoundingMode.FLOOR) * 2
    val rng = new Random()

    // this set of values is virtually guaranteed not
    // to have a CI of +/- 50 which means that it should
    // exceed the budget first
    val mock_answers = Seq.fill(n.toInt)(rng.nextDouble() * 100000).toList

    def getMockList: List[MockAnswer[Double]] = makeMocks[Double](rng.shuffle(mock_answers))

    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.TRACE_MEMOIZE_VERBOSE
    }

    def countJellies(i: Int) = a.EstimationQuestion { q =>
      println(s"(${i}) (${i}) (${i}) (${i}) ####### COUNT THE JELLIES ####### !&!&!&!&!&!&!&!&!&! (${i})")
      q.confidence = 0.95
      q.budget = dollar_budget
      q.wage = wage
      q.text = s"${i}. How many jelly beans are in this jar?"
      q.confidence_interval = SymmetricCI(50)
      q.mock_answers = getMockList
    }

    try {
      val answers = (0 until 15).map { i => countJellies(i) }.toArray

      val outcome = answers.map { a =>
        a.answer match {
          case LowConfidenceEstimate(_,_,_,_,_) => true
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
