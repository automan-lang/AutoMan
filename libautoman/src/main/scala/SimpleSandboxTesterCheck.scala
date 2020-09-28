import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.core.logging.LogLevelDebug
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object SimpleSandboxTesterCheck extends App {
  val opts = Utilities.unsafe_optparse(args, "SimpleSandboxTester")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean,
    log_verbosity = LogLevelDebug(),
    database_path = "dbarowy_saved_hits"
  )

  def which_one() = checkbox (
    budget = 8.00,
    text = "Which of these belong?",
    options = (
      choice('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
      choice('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
      choice('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
      choice('cookiemonster, "Cookie Monster", "http://tinyurl.com/otb6thl"),
      choice('thecount, "The Count", "http://tinyurl.com/nfdbyxa")
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
    which_one().answer match {
      case answer: Answer[Set[Symbol]] =>
        println("The answer is: " + answer.value)
      case lowconf: LowConfidenceAnswer[Set[Symbol]] =>
        println(
          "You ran out of money. The best answer is \"" +
            lowconf.value + "\" with a confidence of " + lowconf.confidence
        )
    }
  }
}