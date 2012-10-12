import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.core.Utilities

object simple_checkbox_program extends App {
  val opts = Utilities.unsafe_optparse(args)

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.budget = 8.00
    mt.sandbox_mode = true
  }

  def which_one(text: String, dual_text: String) = a.CheckboxQuestion { q =>
    q.text = text
    q.dual_text = dual_text
    q.options = List(
      a.Option('oscar, "Oscar the Grouch" /*, "http://tinyurl.com/c6d2s2r" */),
      a.Option('kermit, "Kermit the Frog"/*, "http://tinyurl.com/cujgof6"*/),
      a.Option('spongebob, "Spongebob Squarepants"/*, "http://tinyurl.com/crl84ms"*/),
      a.Option('cookie, "Cookie Monster"/*, "http://tinyurl.com/c8m7wsd"*/),
      a.Option('count, "The Count"/*, "http://tinyurl.com/cf8a7rb"*/)
    )
  }

  val wo_future = which_one("Which of these DO NOT BELONG? (check all that apply)", "Which of these BELONG TOGETHER? (check all that apply)")
  println("answer1 is a " + wo_future())
}