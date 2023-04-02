import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}
import scala.collection.immutable.ListMap

object LindaProblemKTExactLonger extends App {
  val opts = Utilities.unsafe_optparse(args, "LindaProblemKTExactLonger")

  implicit val a: MTurkAdapter = mturk(
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

	def conjunction_fallacy() = Survey(
	  budget = 100.00,
		title = "Answer some short questions that test your cognitive function",
		text = "Please read the following questions and answer them carefully.",
	  questions = List(
			estimateQuestion(
				text = "How many peas would you estimate are on this plate?",
				image_url =  "https://s3.amazonaws.com/edu-umass-cs-automan-2014-07-01-calorie/0715a58400d974009fa79368a05471ac.jpg"
			),
			radioQuestion(
				text = "What time of day is it where you are?",
				options = (
					choice('morning, "Morning."),
					choice('noon, "Noon."),
					choice('afternoon, "Afternoon."),
					choice('night, "Night."),
				)
			),
			checkboxQuestion(
				text = "Which of the following types of vehicles have you personally driven before? (check all that apply, or leave blank)",
				options = (
					choice('sedan, "A four-door sedan."),
					choice('wagon, "A station wagon."),
					choice('suv, "A sport-utility vehicle (SUV)."),
					choice('twuck, "A pickup truck."),
					choice('motorcycle, "A motorcycle."),
					choice('sport, "A sports car."),
				)
			),
			estimateQuestion(
				text = "About how old is your best friend?"
			),
	    radioQuestion(
	      text = "${name} is ${description}. ${pronoun} majored in ${major}. " +
					     "As a student, ${pronoun} ${activity}.<br><br>" +
					     "Which is more probable?",
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
	    "pronoun" -> ("name", ListMap(
	      "Linda" -> "she",
	      "Bill"  -> "he")),
	    "major"   -> ("name", ListMap(
	      "Linda" -> "philosophy",
	      "Bill"  -> "accounting")),
	    "description" -> ("name", ListMap(
	      "Linda" -> "31 years old, single, outspoken, and very bright",
	      "Bill"  -> "intelligent, but unimaginative, compulsive, and generally lifeless")),
	    "activity" -> ("name", ListMap(
	      "Linda" -> "was concerned with issues of discrimination and social justice, and also participated in anti-nuclear demonstrations",
	      "Bill"  -> "was strong in mathematics but weak in social studies and humanities")),
	    "profession" -> ("name", ListMap("Linda" -> "bank teller", "Bill" -> "physician")),
	    "attribute"   -> ("name", ListMap(
	      "Linda" -> "is active in the feminist movement",
	      "Bill"  -> "plays jazz for a hobby"))
	  ),
		sample_size = 200,
		initial_worker_timeout_in_s = 300,
		question_timeout_multiplier = 180, // used to calculate the length of an epoch before producing a timeout
		noise_percentage = 0.4,
		cohen_d_threshold = -1000,
		wage = 7.25 / 5,
		csv_output = "linda_exact_longer" + java.time.LocalDateTime.now.toString + ".csv"
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
