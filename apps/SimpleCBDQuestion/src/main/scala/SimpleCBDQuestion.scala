import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import edu.umass.cs.automan.core.question.QuestionOption

object SimpleCBDQuestion extends App {
  val sample_size = 3
  
  val opts = Utilities.unsafe_optparse(args, "SimpleCBDQuestion.scala")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def AskIt(question: String) = checkboxes (
    sample_size = sample_size,
    text = question,
    options = List[QuestionOption](
//      new MTQuestionOption(null, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
//      new MTQuestionOption(null, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
//      new MTQuestionOption(null, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
//      new MTQuestionOption(null, "Cookie Monster", "http://tinyurl.com/otb6thl"),
//      new MTQuestionOption(null, "The Count", "http://tinyurl.com/nfdbyxa")
      "Oscar the Grouch" -> "http://tinyurl.com/qfwlx56",
      "Kermit the Frog" -> "http://tinyurl.com/nuwyz3u",
      "Spongebob Squarepants" -> "http://tinyurl.com/oj6wzx6",
      "Cookie Monster" -> "http://tinyurl.com/otb6thl",
      "The Count" -> "http://tinyurl.com/nfdbyxa"
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
    val outcome = AskIt("Which of these characters do you know?")
 
    outcome.answer match {
      case a:Answers[Set[Symbol]] =>
        a.values.foreach { case (worker_id, answers) => println("Worker ID: " + worker_id + ", Answer: " + answers.mkString(", ")) }
      case a:IncompleteAnswers[Set[Symbol]] =>
        println("Ran out of money!  Only have " + a.values.size + " of " + sample_size + " responses.")
        a.values.foreach { case (worker_id, answers) => println("Worker ID: " + worker_id + ", Answer: " + answers.mkString(", ")) }
      case a:OverBudgetAnswers[Set[Symbol]] =>
        println("Over budget.  Need: $" + a.need + ", have: $" + a.have)
    }
  }
}
