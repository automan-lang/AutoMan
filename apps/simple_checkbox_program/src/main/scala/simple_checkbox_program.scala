import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object simple_checkbox_program extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_checkbox_program.jar")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def which_one(text: String) = a.CheckboxQuestion { q =>
    q.budget = 8.00
    q.text = text
    q.options = List(
      a.Option('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
      a.Option('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
      a.Option('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
      a.Option('cookie, "Cookie Monster", "http://tinyurl.com/otb6thl"),
      a.Option('count, "The Count", "http://tinyurl.com/nfdbyxa")
    )
    q.minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  }

  automan(a) {
    val outcome = which_one("Which of these DO NOT BELONG? (check all that apply)")
  
    outcome.answer match {
      case a:Answer[Set[Symbol]] =>
        println("Answers are: " + a.value.map(_.toString).mkString(","))
      case _ => println("Error occurred.")
    }
  }
}