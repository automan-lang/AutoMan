import edu.umass.cs.automan.adapters.mturk._

object simple_program extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def which_one() = a.RadioButtonQuestion { q =>
    q.budget = 8.00
    q.text = "Which one of these does not belong?"
    q.options = List(
      a.Option('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
      a.Option('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
      a.Option('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
      a.Option('cookie, "Cookie Monster", "http://tinyurl.com/otb6thl"),
      a.Option('count, "The Count", "http://tinyurl.com/nfdbyxa")
    )
  }

  automan(a) {
    which_one().answer match {
      case Answer(value, _, _) =>
        println("The answer is: " + value)
      case LowConfidenceAnswer(value, cost, conf) =>
        println(
          "You ran out of money. The best answer is \"" +
          value + "\" with a confidence of " + conf
        )
    }
  }
}