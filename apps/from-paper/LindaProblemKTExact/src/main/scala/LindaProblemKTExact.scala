import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}

import scala.collection.immutable.ListMap

object LindaProblemKTExact extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a: MTurkAdapter = mturk(
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

	def conjunction_fallacy() = Survey(
	  budget = 100.00,
	  sample_size = 200,
	  title = "Which is more probable?",
	  text = "${name} is ${description}. ${pronoun} majored in ${major}. " +
	         "As a student, ${pronoun} ${activity}.",
	  questions = List(
	    radioQuestion(
	      text = "Which is more probable?",
	      options = (
	        choice('A, "${name} is a ${profession}"),
	        choice('B, "${name} is a ${profession} and ${attribute}")
				)
			),
	  ),
		words_candidates = ListMap(
			"name" -> Array("Linda", "Bill"),
		),
	  functions = ListMap(
	    "pronoun" -> ("name", Map(
	      "Linda" -> "she",
	      "Bill"  -> "he")),
	    "major"   -> ("name", Map(
	      "Linda" -> "philosophy",
	      "Bill"  -> "accounting")),
	    "description" -> ("name", Map(
	      "Linda" -> "31 years old, single, outspoken, and very bright",
	      "Bill"  -> "intelligent, but unimaginative, compulsive, and generally lifeless")),
	    "activity" -> ("name", Map(
	      "Linda" -> "was concerned with issues of discrimination and social justice, and also participated in anti-nuclear demonstrations",
	      "Bill"  -> "was strong in mathematics but weak in social studies and humanities")),
	    "profession" -> ("name", Map("Linda" -> "bank teller", "Bill" -> "physician")),
	    "attribute"   -> ("name", Map(
	      "Linda" -> "is active in the feminist movement",
	      "Bill"  -> "plays jazz for a hobby"))
	  ),
    csv_output = "linda_" + java.time.LocalDateTime.now.toString + ".csv",
    initial_worker_timeout_in_s = 60,
    question_timeout_multiplier = 180,
    noise_percentage = 0.4,
    cohen_d_threshold = -1 
  )

  automan(a) {
    /* We use pattern-matching to handle exceptional outcomes.
     * Refer to the API documentation for cases:
     *   https://docs.automanlang.org/technical-documentation/automan-api-reference
     */

    conjunction_fallacy().saveToCSV()

    conjunction_fallacy().answer match {
      case answer: SurveyAnswers[List[Any]] =>
        println("The answer is: " + answer.values)
      case incomplete: SurveyIncompleteAnswers[List[Any]] =>
        // TODO: what about low confidence?
        println("The incomplete answer is: " + incomplete.values)
      case oba: SurveyOverBudgetAnswers[List[Any]] =>
        println("You have $" + oba.have + " but you need $" + oba.need +
          " to start this task.");
      case _ => println("Something went wrong!")
    }
  }
}