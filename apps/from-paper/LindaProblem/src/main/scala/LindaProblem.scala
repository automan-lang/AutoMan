import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}

import scala.collection.immutable.ListMap

object LindaProblem extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a: MTurkAdapter = mturk(
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def which_one(): DSL.SurveyOutcome[List[Any]] = Survey(
    questions = List(
      radioQuestion(
        text = "Which is more probable?",
        options = (
          choice('A, "${name} is a bank teller"),
          choice('B, "${name} is a bank teller and is active in the feminist movement"),
        )
      ),
    ),
    budget = 100.00,  // this field is a hard limit per question/survey on how much the user will pay
    // (the survey will terminate if total price of tasks increase beyond this limit and throw OverBudgetException`)
    // TODO: set a different default value/function based on survey
    csv_output = "linda_" + java.time.LocalDateTime.now.toString + ".csv",
    title = "Which is more probable?",
    text = "${name} is ${description}. ${gender} majored in ${major}. As a student, ${gender} ${activity}.",
    words_candidates = ListMap[String, Array[String]](
      "name" -> Array("Linda", "Bill"),
      "description" -> Array("31 years old", "single", "outspoken", "very bright"),
      "major" -> Array("philosophy","computer science","comparative literature","economics","psychology"),
      "activity" -> Array("was deeply concerned with issues of discrimination and social justice", "participated in anti-nuclear demonstrations")
    ),
    functions = ListMap("gender" -> ("name", Map("Linda"->"she", "Bill"->"he"))),
    sample_size = 10,
    initial_worker_timeout_in_s = 320,
    wage=3.625,
    question_timeout_multiplier = 180,  // used to calculate the time of an epoch determining "TIMEOUT" sate
  )

  automan(a) {
    /* We use pattern-matching to handle exceptional outcomes.
     * Refer to the API documentation for cases:
     *   https://docs.automanlang.org/technical-documentation/automan-api-reference
     */

    // println(which_one().answer)
    which_one().saveToCSV()

    which_one().answer match {
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
