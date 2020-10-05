package org.automanlang.core.scheduler

import org.automanlang.core.logging.LogLevelDebug
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy
import org.scalatest._
import java.util.UUID
import org.automanlang.test._
import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.mock.MockSetup

class TimeoutTest extends FlatSpec with Matchers {
  "A radio button program" should "timeout and double reward" in {
    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.NO_LOGGING,
      log_verbosity = LogLevelDebug()
    )

    def which_one() = radio (
      budget = 5.00,
      text = "Which one of these does not belong?",
      // make sure that this task times out after exactly 30s
      initial_worker_timeout_in_s = 30,
      question_timeout_multiplier = 1,
      options = List(
        choice('oscar, "Oscar the Grouch"),
        choice('kermit, "Kermit the Frog"),
        choice('spongebob, "Spongebob Squarepants"),
        choice('cookie, "Cookie Monster"),
        choice('count, "The Count")
      ),
      mock_answers = makeTimedMocks(
        List(
          ('spongebob, 0),
          ('spongebob, 45000),
          ('spongebob, 45000),
          ('spongebob, 45000),
          ('spongebob, 45000),
          ('spongebob, 45000)
        )
      ),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    automan(mt, test_mode = true) {
      which_one().answer match {
        case Answer(value, cost, conf, _, _) =>
          println("Answer: '" + value + "', cost: '" + cost + "', confidence: " + conf)
          (value == 'spongebob) should be (true)
          cost should be (BigDecimal(0.06) * BigDecimal(1) + BigDecimal(0.12) * BigDecimal(4))
          (conf > 0.95) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
