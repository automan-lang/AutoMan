package edu.umass.cs.automan.core.logging

import java.util.UUID
import edu.umass.cs.automan.core.answer.LowConfidenceEstimate
import edu.umass.cs.automan.core.mock.MockAnswer
import scala.util.Random
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.tagobjects.Slow
import org.scalatest.Ignore

@Ignore
class H2KillerTest extends FlatSpec with Matchers {
  "An estimator program" should "not experience a database primary key error under load" taggedAs Slow in {
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

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.TRACE_MEMOIZE_VERBOSE,
      log_verbosity = LogLevelDebug()
    )

    def countJellies(i: Int) = estimate (
      confidence = 0.95,
      budget = dollar_budget,
      wage = wage,
      text = s"${i}. How many jelly beans are in this jar?",
      confidence_interval = SymmetricCI(50),
      mock_answers = getMockList
    )

    automan(mt, test_mode = true, in_mem_db = true) {
      try {
        val answers = (0 until 15).map { i => countJellies(i) }.toArray

        val outcome = answers.map { a =>
          a.answer match {
            case LowConfidenceEstimate(_, _, _, _, _, _, _) => true
            case _ => false
          }
        }.foldLeft(true) { case (acc, v) => acc && v }

        // all estimates should be over budget.
        // if anything else happens, the test failed
        outcome should be(true)
      } catch {
        case _: Throwable => fail()
      }
    }
  }
}
