import edu.umass.cs.automan.adapters.mturk._

object SimpleFTDQuestion extends App {
  val sample_size = 3
  
  val opts = Utilities.unsafe_optparse(args, "SimpleFTDQuestion.scala")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def AskIt(question: String) = a.FreeTextDistributionQuestion { q =>
    q.sample_size = sample_size
    q.title = question
    q.text = question
  }

  automan(a) {
    val outcome = AskIt("How many licks does it take to get to the Tootsie Roll Center of a Tootsie Pop?")

    outcome.answer match {
      case Answers(answers,_) =>
        answers.map { case (worker_id, answer) => println("Worker ID: " + worker_id + ", Answer: " + answer.toString()) }
      case IncompleteAnswers(answers,_) =>
        println("Ran out of money!  Only have " + answers.size + " of " + sample_size + " responses.")
        answers.map { case (worker_id, answer) => println("Worker ID: " + worker_id + ", Answer: " + answer.toString()) }
      case OverBudgetAnswers(need,have) => println("Over budget.  Need: $" + need + ", have: $" + have)
    }
  }
}
