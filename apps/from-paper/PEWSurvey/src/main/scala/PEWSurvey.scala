import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}

object PEWSurvey extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a: MTurkAdapter = mturk(
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def which_one(): DSL.SurveyOutcome[List[Any]] = Survey(
    questions = List(
      radioQuestion(
        text = "Saving for the future",
        options = (
          choice('Easier, "Easier"),
          choice('Harder, "Harder"),
          choice('Same, "About the same")
        )
      ),
      radioQuestion(
        text = "Paying for college",
        options = (
          choice('Easier, "Easier"),
          choice('Harder, "Harder"),
          choice('Same, "About the same")
        )
      ),
      radioQuestion(
        text = "Buying a home",
        options = (
          choice('Easier, "Easier"),
          choice('Harder, "Harder"),
          choice('Same, "About the same")
        )
      ),
      radioQuestion(
        text = "Finding a spouse or partner",
        options = (
          choice('Easier, "Easier"),
          choice('Harder, "Harder"),
          choice('Same, "About the same")
        )
      ),
      radioQuestion(
        text = "Finding a job",
        options = (
          choice('Easier, "Easier"),
          choice('Harder, "Harder"),
          choice('Same, "About the same")
        )
      ),
      radioQuestion(
        text = "Getting into college",
        options = (
          choice('Easier, "Easier"),
          choice('Harder, "Harder"),
          choice('Same, "About the same")
        )
      ),
      radioQuestion(
        text = "Staying in touch with family and friends",
        options = (
          choice('Easier, "Easier"),
          choice('Harder, "Harder"),
          choice('Same, "About the same")
        )
      ),
      radioQuestion(
        text = "Do you currently live in the United States?",
        options = (
          choice('Yes, "Yes"),
          choice('No, "No")
        )
      ),
      estimateQuestion(
        confidence_interval = UnconstrainedCI(),
        text = "What is your age?"
      )
    ),
    budget = 150.00,  // this field is a hard limit per question/survey on how much the user will pay
    // (the survey will terminate if total price of tasks increase beyond this limit and throw OverBudgetException`)
    // TODO: set a different default value/function based on survey
    csv_output = "pew_" + java.time.LocalDateTime.now.toString + ".csv",
    title = "How do you think young adults today compare with their parents\' generation on each of the following?",
    text = "For the following questions, is this action easier, harder, or the same for young adults today compared to their parents\' generation?",
    //    text = "PEWHTMLsurvey",
    //    sample_size = 300,
    //    minimum_spawn_policy = UserDefinableSpawnPolicy(0),
    sample_size = 300,
    initial_worker_timeout_in_s = 160,
    question_timeout_multiplier = 180,  // used to calculate the time of an epoch determining "TIMEOUT" sate
  )

  automan(a) {
    /* We use pattern-matching to handle exceptional outcomes.
     * Refer to the API documentation for cases:
     *   https://docs.automanlang.org/technical-documentation/automan-api-reference
     */

    // println(which_one().answer)
//    which_one().saveToCSV()

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
