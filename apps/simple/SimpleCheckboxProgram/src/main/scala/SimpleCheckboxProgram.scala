import org.automanlang.adapters.mturk.DSL._
import org.automanlang.core.logging.LogLevelDebug
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy

object SimpleCheckboxProgram extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_checkbox_program")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean,
    log_verbosity = LogLevelDebug()
  )

  def which_one(text: String) = checkbox (
    budget = 8.00,
    text = text,
    options = List[MTQuestionOption](
      "Oscar the Grouch" -> "http://tinyurl.com/qfwlx56",
      "Kermit the Frog" -> "http://tinyurl.com/nuwyz3u",
      "Spongebob Squarepants" -> "http://tinyurl.com/oj6wzx6",
      "Cookie Monster" -> "http://tinyurl.com/otb6thl",
      "The Count" -> "http://tinyurl.com/nfdbyxa"
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
    val outcome = which_one("Which of these DO NOT BELONG? (check all that apply)")
  
    outcome.answer match {
      case a:Answer[Set[Symbol]] =>
        println("Answers are: " + a.value.map(_.toString).mkString(","))
      case _ => println("Error occurred.")
    }
  }
}