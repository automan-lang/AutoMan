//import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
//import edu.umass.cs.automan.automan
//import edu.umass.cs.automan.core.Utilities
//import scala.concurrent.Await
//import scala.concurrent.duration.Duration

object SimpleFTDQuestion extends App {
//  val opts = Utilities.unsafe_optparse(args, "SimpleFTDQuestion.scala")
//
//  // init MTurk backend
//  val a = MTurkAdapter { mt =>
//    mt.access_key_id = opts('key)
//    mt.secret_access_key = opts('secret)
//    mt.sandbox_mode = opts('sandbox).toBoolean
//  }
//
//  def AskIt(question: String) = a.FreeTextDistributionQuestion { q =>
//    q.num_samples = 3
//    q.title = question
//    q.text = question
//  }
//
//  val answers = automan(a) {
//    val future_answers = AskIt("How many licks does it take to get to the Tootsie Roll Center of a Tootsie Pop?")
//    Await.result(future_answers, Duration.Inf)
//  }
//
//  answers.map { answer => println("Answer: " + answer.toString())}
}
