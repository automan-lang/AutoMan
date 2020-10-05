package org.automanlang.core.scheduler

import org.automanlang.core.logging.LogLevelDebug
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy
import org.scalatest._
import java.util.UUID
import org.automanlang.test._
import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.mock.MockSetup

class ComplicatedTimeoutTest extends FlatSpec with Matchers {
  "A radio button program" should "timeout and a bunch of stuff should happen" in {
    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.NO_LOGGING,
      log_verbosity = LogLevelDebug()
    )

    def which_one() = radio (
      budget = 10.00,
      text = "ComplicatedTimeoutTest?",
      // make sure that this task times out after exactly 30s
      initial_worker_timeout_in_s = 30,
      question_timeout_multiplier = 1,
      options = List(
        choice('z, "Z"),
        choice('zz, "ZZ"),
        choice('a, "A"),
        choice('zzz, "ZZZ"),
        choice('zzzz, "ZZZZ")
      ),
      mock_answers = makeTimedMocks(
        List(
          ('a,     0),
          ('z,     1),
          ('zz,    3),
          ('a,    45),
          ('z,    45),
          ('zzz,  45),
          ('zzzz, 45),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65),
          ('a,    65)
        )
      ),
      minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    )

    automan(mt, test_mode = true) {
      which_one().answer match {
        case Answer(ans, cost, conf, qid, _) =>
          println("question_id = " + qid + ", Answer: '" + ans + "', cost: '" + cost + "', confidence: " + conf)
          (ans == 'a) should be (true)
          (cost >= BigDecimal(2.34)) should be (true)
          (conf > 0.95) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
