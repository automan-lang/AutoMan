import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.core.Utilities
import edu.umass.cs.automan.core.exception.OverBudgetException
import scala.concurrent._
import scala.concurrent.duration._

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
      a.Option('oscar, "Oscar the Grouch" /*, "http://tinyurl.com/c6d2s2r" */),
      a.Option('kermit, "Kermit the Frog"/*, "http://tinyurl.com/cujgof6"*/),
      a.Option('spongebob, "Spongebob Squarepants"/*, "http://tinyurl.com/crl84ms"*/),
      a.Option('cookie, "Cookie Monster"/*, "http://tinyurl.com/c8m7wsd"*/),
      a.Option('count, "The Count"/*, "http://tinyurl.com/cf8a7rb"*/)
    )
  }

  try {
    val future_answer = which_one("Which of these DO NOT BELONG? (check all that apply)")
    val answers: Set[Symbol] = Await.result(future_answer, Duration.Inf).values
    println("answer1 is a " + answers.map(_.toString).mkString(","))

  } catch {
    case OverBudgetException(e) => println("Over budget!")
  }
}