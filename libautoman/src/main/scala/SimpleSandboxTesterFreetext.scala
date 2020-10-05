import org.automanlang.adapters.mturk.DSL._
import org.automanlang.core.logging.LogLevelDebug
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy
import org.automanlang.core.util.Utilities

object SimpleSandboxTesterFreetext extends App {
  val opts = Utilities.unsafe_optparse(args, "SimpleSandboxTester")

  implicit val a = mturk(
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean,
    log_verbosity = LogLevelDebug(),
    database_path = "dbarowy_saved_hits"
  )

  def which_one() = freetext(
    budget = 8.00,
    text = "What is your favorite number?",
    pattern = "YYYYYYYX",
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
    which_one().answer match {
      case answer: Answer[String] =>
        println("The answer is: " + answer.value)
      case lowconf: LowConfidenceAnswer[String] =>
        println(
          "You ran out of money. The best answer is \"" +
            lowconf.value + "\" with a confidence of " + lowconf.confidence
        )
    }
  }
}
