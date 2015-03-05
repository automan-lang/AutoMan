import edu.umass.cs.automan.adapters.mturk.MTurkAdapter
import edu.umass.cs.automan.automan
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.util.Utilities

object simple_program extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program.jar")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  automan(a) {
    def which_one() = a.RadioButtonQuestion { q =>
      q.budget = 8.00
      q.text = "Which one of these does not belong?"
      q.options = List(
        a.Option('oscar, "Oscar the Grouch"),
        a.Option('kermit, "Kermit the Frog"),
        a.Option('spongebob, "Spongebob Squarepants"),
        a.Option('cookie, "Cookie Monster"),
        a.Option('count, "The Count")
      )
    }

    which_one().answer match {
      case ScalarAnswer(value, _, _) =>
        println("The answer is: " + value)
      case ScalarOverBudget(value, cost, conf) =>
        println(
          "You ran out of money. The best answer is \"" +
          value + "\" with a confidence of " + conf
        )
    }
  }
}