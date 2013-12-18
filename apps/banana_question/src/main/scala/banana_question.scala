import edu.umass.cs.automan.adapters.MTurk._
import edu.umass.cs.automan.core.Utilities

object banana_question extends App {
  val opts = Utilities.unsafe_optparse(args, "banana_question.jar")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = true
  }

  def which_one(text: String) = a.RadioButtonQuestion { q =>
    q.budget = 5.00
    q.text = text
    q.options = List(
      a.Option('banana, "Banana", "http://tinyurl.com/7u5d4n8"),
      a.Option('apple, "Apple", "http://tinyurl.com/7egjghy"),
      a.Option('celery, "Celery", "http://tinyurl.com/7df3z6s"),
      a.Option('cucumber, "cucumber", "http://tinyurl.com/77bgukv"),
      a.Option('orange, "orange", "http://tinyurl.com/77y7o57")
    )
    q.question_timeout_multiplier = 20
  }

  val wo_future = which_one("Which one of these does not belong?")
  println("answer1 is: " + wo_future())
  if (wo_future().over_budget) {
    println("Over budget!; answer has confidence of: " + wo_future().confidence)
  }
}