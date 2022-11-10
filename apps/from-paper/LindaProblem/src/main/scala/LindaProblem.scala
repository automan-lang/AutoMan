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
          choice('A, "${choiceA}"),
          choice('B, "${choiceB}"),
        )
      ),
    ),
    budget = 50.00,  // this field is a hard limit per question/survey on how much the user will pay
    // (the survey will terminate if total price of tasks increase beyond this limit and throw OverBudgetException`)
    // TODO: set a different default value/function based on survey
    csv_output = "linda_" + java.time.LocalDateTime.now.toString + ".csv",
    title = "Which is more probable?",
    text = "${description}",
    words_candidates = ListMap[String, Array[String]](
      "name" -> Array("Linda", "Bill"),
      "description" -> Array("31 years old", "single", "outspoken", "very bright"),
      "major" -> Array("philosophy","computer science","comparative literature","economics","psychology"),
      "activity" -> Array("was deeply concerned with issues of discrimination and social justice", "participated in anti-nuclear demonstrations")
    ),
    functions = ListMap(
      "description" -> ("name", Map(
        "Linda"->"Linda is 31 years old, single, outspoken, and very bright. She majored in philosophy. As a student, she was deeply concerned with issues of discrimination and social justice, and also participated in anti-nuclear demonstrations.",
        "Bill"->"Bill is 34 years old. He is intelligent, but unimaginative, compulsive and generally lifeless. In school, he was strong in mathematics but weak in social studies and humanities."
      )),
      "choiceA" -> ("name", Map(
        "Linda" -> "Linda is a bank teller.",
        "Bill" -> "Bill plays jazz for a hobby."
      )),
      "choiceB" -> ("name", Map(
        "Linda" -> "Linda is a bank teller and is active in the feminist movement.",
        "Bill" -> "Bill is an accountant that plays jazz for a hobby."
      )),
    ),
    sample_size = 100,
    initial_worker_timeout_in_s = 120,
    wage=7.25,
    question_timeout_multiplier = 180,  // used to calculate the time of an epoch determining "TIMEOUT" sate
    noise_percentage = 0.4,
    cohen_d_threshold = -1
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
