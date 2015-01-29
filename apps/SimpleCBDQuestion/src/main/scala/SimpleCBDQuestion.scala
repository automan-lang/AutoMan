//import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
//import edu.umass.cs.automan.automan
//import edu.umass.cs.automan.core.Utilities
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration

object SimpleCBDQuestion extends App {
//  val opts = Utilities.unsafe_optparse(args, "SimpleCBDQuestion.scala")
//
//  // init MTurk backend
//  val a = MTurkAdapter { mt =>
//    mt.access_key_id = opts('key)
//    mt.secret_access_key = opts('secret)
//    mt.sandbox_mode = opts('sandbox).toBoolean
//  }
//
//  def AskIt(question: String) = a.CheckboxDistributionQuestion { q =>
//    q.num_samples = 3
//    q.text = question
//    q.options = List(
//      a.Option('oscar, "Oscar the Grouch" /*, "http://tinyurl.com/c6d2s2r" */),
//      a.Option('kermit, "Kermit the Frog"/*, "http://tinyurl.com/cujgof6"*/),
//      a.Option('spongebob, "Spongebob Squarepants"/*, "http://tinyurl.com/crl84ms"*/),
//      a.Option('cookie, "Cookie Monster"/*, "http://tinyurl.com/c8m7wsd"*/),
//      a.Option('count, "The Count"/*, "http://tinyurl.com/cf8a7rb"*/)
//    )
//  }
//
//  val answers = automan(a) {
//    val future_answers = AskIt("Which of these characters do you know?")
//    Await.result(future_answers, Duration.Inf)
//  }
//
//  answers.map { answer => println("Answer: " + answer.toString())}
}
