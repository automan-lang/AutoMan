
import java.util.UUID

import edu.umass.cs.automan.adapters.googleads.DSL._
import edu.umass.cs.automan.adapters.googleads.ads.Account
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object simple_test extends App {

  //val opts = Utilities.unsafe_optparse(args, "simple_program")
  implicit val a = gads(1373958703)

  def which_one() = radio(
    budget = 5.00,
    text = "Which ingredient goes in coffee?",
    options = (
      choice('milk,"Milk"),
      choice('lemon,"Lemon"),
      choice('salt,"Salt"),
      choice('honey,"Honey")
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0),
    title = "Question 180",
    cpc = 0.2
  )

  automan(a) {
    which_one().answer match {
      case answer: Answer[Symbol] =>
        println("The answer is: " + answer.value + ". You spent $" + answer.cost)
      case lowconf: LowConfidenceAnswer[Symbol] =>
        println(
          "You ran out of money. The best answer is \"" +
            lowconf.value + "\" with a confidence of " + lowconf.confidence
        )
    }
  }

}