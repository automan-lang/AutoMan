import com.amazonaws.services.mturk.model.QualificationRequirement
import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}
import scala.collection.immutable.ListMap
import scala.collection.JavaConverters._

object MTurkUtils {
	def getQualification(kind: String, num: Int, is_sandbox: Boolean): QualificationRequirement = {
		kind match {
			case "masters" =>
				val mastersRequirement = new QualificationRequirement();
				val SANDBOX = "2ARFPLSP75KLA8M8DH1HTEQVJT3SY6"
				val PRODUCTION = "2F1QJWKUDD8XADTFD2Q0G6UTO95ALH"
				val qualificationID = if (is_sandbox) SANDBOX else PRODUCTION
				mastersRequirement.setQualificationTypeId(qualificationID)
				mastersRequirement.setComparator("Exists")
				mastersRequirement
			case "percent" =>
				val q = new QualificationRequirement();
				q.setQualificationTypeId("000000000000000000L0")
				q.setComparator("GreaterThan")
				q.setIntegerValues(List(Integer.valueOf(num)).asJava)
				q
			case "numhits" =>
				val q = new QualificationRequirement();
				q.setQualificationTypeId("00000000000000000040")
				q.setComparator("GreaterThan")
				q.setIntegerValues(List(Integer.valueOf(num)).asJava)
				q
		}
	}
}

object LindaProblemKTExactLongerPicsQuals extends App {
  val opts = Utilities.unsafe_optparse(args, "LindaProblemKTExactLongerPicsQuals")

  implicit val a: MTurkAdapter = mturk(
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

	def conjunction_fallacy() = SurveyWithQuals(
		qualifications = List(MTurkUtils.getQualification("percent", 90, a.sandbox_mode)),
	  budget = 100.00,
		title = "Perform a short cognitive function test",
		text = "Read the following questions and answer them carefully.",
	  questions = List(
			estimateQuestion(
				// How many peas would you estimate are on this plate?
				text = "",
				image_url = "https://danaws.s3.amazonaws.com/peas.png"
			),
			radioQuestion(
				// What time of day is it where you are?
				image_url = "https://danaws.s3.amazonaws.com/time.png",
				text = "",
				options = (
					choice('morning, "", "https://danaws.s3.amazonaws.com/Morning.png"),
					choice('noon, "", "https://danaws.s3.amazonaws.com/Noon.png"),
					choice('afternoon, "", "https://danaws.s3.amazonaws.com/Afternoon.png"),
					choice('night, "", "https://danaws.s3.amazonaws.com/Night.png"),
				)
			),
			checkboxQuestion(
				// Which of the following types of vehicles have you personally driven?
				image_url = "https://danaws.s3.amazonaws.com/vehicles.png",
				text = "",
				options = (
					choice('sedan, "", "https://danaws.s3.amazonaws.com/Sedan.png"),
					choice('wagon, "", "https://danaws.s3.amazonaws.com/Wagon.png"),
					choice('suv, "", "https://danaws.s3.amazonaws.com/SUV.png"),
					choice('twuck, "", "https://danaws.s3.amazonaws.com/Twuck.png"),
					choice('motorcycle, "", "https://danaws.s3.amazonaws.com/Motorcycle.png"),
					choice('sport, "", "https://danaws.s3.amazonaws.com/Sports.png"),
				)
			),
			estimateQuestion(
				// how old is your best friend?
				text = "",
				image_url = "https://danaws.s3.amazonaws.com/age.png"
			),
	    radioQuestion(
				// linda question
				image_url = "https://danaws.s3.amazonaws.com/linda.png",
	      text = "",
	      options = (
	        choice('A, "", "https://danaws.s3.amazonaws.com/linda-teller.png"),
	        choice('B, "", "https://danaws.s3.amazonaws.com/linda-feminist.png")
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
