import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.question.MTSurvey
import edu.umass.cs.automan.core.answer.SurveyAnswers
import edu.umass.cs.automan.core.grammar.Expand.{Start, binding, ch, fun, nt, opt, ref, seq, term}
import edu.umass.cs.automan.core.grammar.{CheckboxQuestionProduction, CheckboxesQuestionProduction, EstimateQuestionProduction, Expression, FreetextQuestionProduction, FreetextsQuestionProduction, Name, OptionProduction, RadioQuestionProduction, RadiosQuestionProduction, Sequence, Terminal}
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import edu.umass.cs.automan.core.util.Utilities

// survey with swerve-kills-homeless
object moral_machine_2 extends App {

  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  val scenarioGrammar = Map[Name, Expression](
    Start -> seq(Array(
      term("What should the self-driving car do?"),
      ref("A"),
      ref("B"))),
    nt("A") -> opt(seq(Array(ref("Scene"), ref("Deaths")))),
    nt("B") -> opt(seq(Array(ref("Scene"), ref("Deaths")))),
    nt("Scene") -> seq(
      Array(
        term("In the "),
        ref("Side"),
        term(" case, the self-driving car with sudden brake failure will "),
        ref("Action"), term(" and "),
        ref("DriveorCrash"),
        term(" a "),
        ref("Object"),
        term(". This will result in ...\nDead:\n"))),
    nt("Side") -> ch(Array(
      term("left-hand"),
      term("right-hand")
    )),
    nt("Action") -> ch(Array(
      term("continue ahead"),
      term("swerve")
    )),
    nt("DriveorCrash") -> ch(Array(
      term("drive through"),
      term("crash into")
    )),
    nt("Object") -> ch(Array(
      term("pedestrian crossing"),
      term("concrete barrier")
    )),
    nt("Deaths") -> ch(Array(
      term("2 women\n1 boy\n"),
      term("2 homeless people\n"),
      term("1 male executive\n1 female executive\n")
    ))
  )

  val timezoneGrammar = Map[Name, Expression](
    Start -> term("What timezone are you in? (Please enter three-letter abbreviation, e.g. EST, IST, GMT, UTC, ect.)")
  )

  val question1Prod = RadioQuestionProduction(scenarioGrammar)
  val question2Prod = RadioQuestionProduction(scenarioGrammar)
  val question3Prod = RadioQuestionProduction(scenarioGrammar)
  val question4Prod = FreetextQuestionProduction(timezoneGrammar)

  //"In this case, the self-driving car with sudden brake failure will continue ahead and crash into a concrete barrier. This will result in ...\nDead:\n"
  //"In this case, the self-driving car with sudden brake failure will swerve and drive through a pedestrian crossing in the other lane. This will result in ...\nDead:\n "

  def which_survey() = surveyGrammar(
    budget = 8.00,
    questions = List(
      nop => radioGrammar(
        grammar = scenarioGrammar,
        image_url = "https://mturk-moral-machine.s3.us-east-2.amazonaws.com/swerve_car.png",
        image_alt_text = "If the car continues straight, two women and a boy crossing the street will die. If it swerves, the car will crash into a barrier and two women and a boy will die.",
        //minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = question1Prod,
        depth = 2,
        variant = 45 //todo add variantArr option
      )(nop),
      nop => radioGrammar(
        grammar = scenarioGrammar,
        image_url = "https://mturk-moral-machine.s3.us-east-2.amazonaws.com/swerve_homeless.png",
        image_alt_text = "If the car continues straight, a male executive and a female executive crossing the street will die. If it swerves, two homeless people crossing the street will die.",
        //minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = question3Prod,
        depth = 2,
        variant = 133 //todo add variantArr option
      )(nop),
      nop => freetextGrammar(
        grammar = timezoneGrammar,
        pattern = "XXX",
        pattern_error_text = "Please use 3-letter timezone abbreviation (EST, IST, GMT, etc.)",
        question = question4Prod,
        variant = 1,
        depth = 2
      )(nop)
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(50),
    initial_worker_timeout_in_s = 75,
    text = "2 questions about self-driving cars"
  )

  automan(a) {
    val outcome = which_survey() //which_survey()
    outcome.answer match {
      case answers: SurveyAnswers =>
        println("The answers are: " + answers)// + answers.values)
      case ans: Answer[_] =>
        println("The answers are: " + ans)
      case _ => println("Something went wrong.")
    }
  }
}
