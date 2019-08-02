
import edu.umass.cs.automan.adapters.googleads.DSL._
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object simple_test extends App {

  //val opts = Utilities.unsafe_optparse(args, "simple_program")
  implicit val a = gads(1373958703)

  def which_one() = radio (
    budget = 0,
    wage = 0.75,
    confidence = 0.95,
    text = "Which one of these does not belong?",
    options = (
      choice('oscar, "Oscar the Grouch"),// "http://tinyurl.com/qfwlx56"),
      choice('kermit, "Kermit the Frog"),// "http://tinyurl.com/nuwyz3u"),
      choice('spongebob, "Spongebob Squarepants"),// "http://tinyurl.com/oj6wzx6"),
      choice('cookiemonster, "Cookie Monster"),// "http://tinyurl.com/otb6thl"),
      choice('thecount, "The Count")//, "http://tinyurl.com/nfdbyxa")
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0),
    title = "Question 104"
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