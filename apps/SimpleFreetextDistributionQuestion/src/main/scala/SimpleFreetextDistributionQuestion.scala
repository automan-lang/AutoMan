import org.automanlang.adapters.mturk.DSL._
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy

object SimpleFreetextDistributionQuestion extends App {
  val sample_size = 3
  
  val opts = Utilities.unsafe_optparse(args, "SimpleFreetextDistributionQuestion.scala")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def AskIt(question: String) = freetexts (
    sample_size = sample_size,
    title = question,
    text = question,
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

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
