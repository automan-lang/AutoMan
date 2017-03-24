package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.logging.LogLevelDebug
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class ComplicatedTimeoutTest extends FlatSpec with Matchers {
  "A radio button program" should "timeout and a bunch of stuff should happen" in {
    val a = MTurkAdapter { mt =>
      mt.access_key_id = UUID.randomUUID().toString
      mt.secret_access_key = UUID.randomUUID().toString
      mt.use_mock = MockSetup(budget = 8.00)
      mt.logging = LogConfig.NO_LOGGING
      mt.log_verbosity = LogLevelDebug()
    }

    def which_one() = a.RadioButtonQuestion { q =>
      q.budget = 10.00
      q.text = "ComplicatedTimeoutTest?"
      // make sure that this task times out after exactly 30s
      q.initial_worker_timeout_in_s = 30
      q.question_timeout_multiplier = 1
      q.options = List(
        a.Option('z, "Z"),
        a.Option('zz, "ZZ"),
        a.Option('a, "A"),
        a.Option('zzz, "ZZZ"),
        a.Option('zzzz, "ZZZZ")
      )
      q.mock_answers = makeTimedMocks(
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
      )
      q.minimum_spawn_policy = UserDefinableSpawnPolicy(0)
    }

    automan(a, test_mode = true) {
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
