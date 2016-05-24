import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

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
    q.minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  }

  automan(a) {
    val outcome = AskIt("How many licks does it take to get to the Tootsie Roll Center of a Tootsie Pop?")

    outcome.answer match {
      case a:Answers[String] =>
        a.values.foreach { case (worker_id, answer) =>
          println("Worker ID: " + worker_id + ", Answer: " + answer)
        }
      case a:IncompleteAnswers[String] =>
        println("Ran out of money!  Only have " + a.values.size + " of " + sample_size + " responses.")
        a.values.foreach { case (worker_id, answer: String) =>
          println("Worker ID: " + worker_id + ", Answer: " + answer)
        }
      case a:OverBudgetAnswers[String] =>
        println("Over budget.  Need: $" + a.need + ", have: $" + a.have)
    }
  }
}
