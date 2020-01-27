import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object simple_survey extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def which_one(text: String) = survey (
    budget = 8.00,
    text = text,
    questions = List[Outcome[_]](
      radio (
        text = "Which one of these does not belong?",
        options = (
          choice('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
          choice('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
          choice('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
          choice('cookiemonster, "Cookie Monster", "http://tinyurl.com/otb6thl"),
          choice('thecount, "The Count", "http://tinyurl.com/nfdbyxa")
        )//,
        //minimum_spawn_policy = UserDefinableSpawnPolicy(0)
      ),
    radio (
      text = "Which one of these likes cookies the most?",
      options = (
        choice('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
        choice('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
        choice('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
        choice('cookiemonster, "Cookie Monster", "http://tinyurl.com/otb6thl"),
        choice('thecount, "The Count", "http://tinyurl.com/nfdbyxa")
      )//,
      //minimum_spawn_policy = UserDefinableSpawnPolicy(0)
      )
    )
  )

  automan(a) {
    which_one("Please take this sample survey.").answer match {
      case answer: Answer[_] =>
        println("The answer is: " + answer.value)
      case lowconf: LowConfidenceAnswer[_] =>
        println(
          "You ran out of money. The best answer is \"" +
          lowconf.value + "\" with a confidence of " + lowconf.confidence
        )
    }
  }
}