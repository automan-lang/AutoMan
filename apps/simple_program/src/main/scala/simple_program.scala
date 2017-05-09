import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object simple_program extends App {
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
      "Oscar the Grouch" -> "http://tinyurl.com/qfwlx56",
      "Kermit the Frog" -> "http://tinyurl.com/nuwyz3u",
      "Spongebob Squarepants" -> "http://tinyurl.com/oj6wzx6",
      "Cookie Monster" -> "http://tinyurl.com/otb6thl",
      "The Count" -> "http://tinyurl.com/nfdbyxa"
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
    which_one().answer match {
      case answer: Answer[Symbol] =>
        println("The answer is: " + answer.value)
      case lowconf: LowConfidenceAnswer[Symbol] =>
        println(
          "You ran out of money. The best answer is \"" +
          lowconf.value + "\" with a confidence of " + lowconf.confidence
        )
    }
  }
}