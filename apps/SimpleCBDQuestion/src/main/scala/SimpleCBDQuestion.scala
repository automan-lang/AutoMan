import edu.umass.cs.automan.adapters.mturk._

object SimpleCBDQuestion extends App {
  val sample_size = 3
  
  val opts = Utilities.unsafe_optparse(args, "SimpleCBDQuestion.scala")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def AskIt(question: String) = a.CheckboxDistributionQuestion { q =>
    q.sample_size = sample_size
    q.text = question
    q.options = List(
      a.Option('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
      a.Option('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
      a.Option('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
      a.Option('cookie, "Cookie Monster", "http://tinyurl.com/otb6thl"),
      a.Option('count, "The Count", "http://tinyurl.com/nfdbyxa")
    )
  }

  automan(a) {
    val outcome = AskIt("Which of these characters do you know?")
 
    outcome.answer match {
      case Answers(answers,_) =>
        answers.map { case (worker_id, answer) => println("Worker ID: " + worker_id + ", Answer: " + answer.mkString(", ")) }
      case IncompleteAnswers(answers,_) =>
        println("Ran out of money!  Only have " + answers.size + " of " + sample_size + " responses.")
        answers.map { case (worker_id, answer) => println("Worker ID: " + worker_id + ", Answer: " + answer.mkString(", ")) }
      case OverBudgetAnswers(need,have) => println("Over budget.  Need: $" + need + ", have: $" + have)
    }
  }
}
