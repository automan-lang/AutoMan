import edu.umass.cs.automan.adapters.MTurk._
import edu.umass.cs.automan.core.Utilities

object simple_program extends App {
  val opts = Utilities.unsafe_optparse(args)

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = true
  }

  def which_one(text: String) = a.RadioButtonQuestion { q =>
    q.budget = 8.00
    q.text = text
    q.options = List(
      a.Option('oscar, "Oscar the Grouch"),
      a.Option('kermit, "Kermit the Frog"),
      a.Option('spongebob, "Spongebob Squarepants"),
      a.Option('cookie, "Cookie Monster"),
      a.Option('count, "The Count")
    )
    q.question_timeout_multiplier = 20
  }

  val wo_future = which_one("Which one of these does not belong?")
  println("answer1 is a " + wo_future())
  if (wo_future().over_budget) {
    println("Over budget!")
  }
}