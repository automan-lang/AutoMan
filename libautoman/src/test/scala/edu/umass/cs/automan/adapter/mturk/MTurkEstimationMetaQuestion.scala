package edu.umass.cs.automan.adapter.mturk

import edu.umass.cs.automan.core.answer.Estimate
import edu.umass.cs.automan.core.logging.LogLevelDebug
import java.util.UUID
import edu.umass.cs.automan.RetryOnceTest
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup
import scala.util.Random
import org.scalatest.tagobjects.Retryable

class MTurkEstimationMetaQuestion extends RetryOnceTest {

  "An estimation program" should "work" taggedAs Retryable in {
    val confidence = 0.95
    val ci = SymmetricCI(50)
    val rng = new Random()

    implicit val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.log_verbosity = LogLevelDebug()
    }

    automan(a, test_mode = true) {
      def candyCount(things: String) = a.EstimationQuestion { q =>
        q.confidence = confidence
        q.budget = 8.00
        q.text = s"How many $things are in my belly?"
        q.confidence_interval = ci
        val n = 500
        val sd = 100
        val mean = 633
        q.mock_answers = makeMocks((0 until n).map {
          i => rng.nextGaussian() * sd + mean
        }.toList)
      }

      val combinedCount = candyCount("jelly beans").combineWith(candyCount("M&Ms"))(e1 => e2 => e1 + e2)

      combinedCount.answer match {
        case e:Estimate =>
          println("Estimate: " + e.value + ", low: " + e.low + ", high: " + e.high + ", cost: $" + e.cost + ", confidence: " + e.confidence)
          (e.value - e.low <= ci.error) should be (true)
          (e.high - e.value <= ci.error) should be (true)
          (e.value > e.low && e.value < e.high) should be (true)
          (e.confidence >= confidence) should be (true)
          (e.cost >= BigDecimal(0.48)) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
