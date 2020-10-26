import org.automanlang.adapters.mturk.DSL._
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy

object SimpleRadioProgram extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def which_one() = radio (
    budget = 8.00,
    text = "Which one of these does not belong?",
    options = (
      choice('oscar, "Oscar the Grouch", "https://tinyurl.com/y2nf2h76"),
      choice('kermit, "Kermit the Frog", "https://tinyurl.com/yxh2emmr"),
      choice('spongebob, "Spongebob Squarepants", "https://tinyurl.com/y3uv2oew"),
      choice('cookiemonster, "Cookie Monster", "https://tinyurl.com/y68x9zvx"),
      choice('thecount, "The Count", "https://tinyurl.com/y6na5a8a")
    ),
    /* The following setting instructs AutoMan to allow HITs with
     * fewer than 10 assignments; the tradeoff is that no batch may
     * ever have more than 10 assignments :(
     */
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
    /* We use pattern-matching to handle exceptional outcomes.
     * Refer to the API documentation for cases:
     *   https://docs.automanlang.org/technical-documentation/automan-api-reference
     */
    which_one().answer match {
      case answer: Answer[Symbol] =>
        println("The answer is: " + answer.value)
      case lowconf: LowConfidenceAnswer[Symbol] =>
        println("You ran out of money. The best answer is \"" +
                lowconf.value + "\" with a confidence of " + lowconf.confidence)
      case oba: OverBudgetAnswer[Symbol] =>
        println("You have $" + oba.have + " but you need $" + oba.need +
	        " to start this task.");
      case _ => println("Something went wrong!")
    }
  }
}