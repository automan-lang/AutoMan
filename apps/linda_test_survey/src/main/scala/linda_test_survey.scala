import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.MTurkAdapter
import edu.umass.cs.automan.adapters.mturk.question.MTSurvey
import edu.umass.cs.automan.core.answer.{SurveyAnswers, SurveyOutcome}
import edu.umass.cs.automan.core.grammar.Expand.{Start, binding, ch, fun, nt, opt, ref, seq, term}
import edu.umass.cs.automan.core.grammar.{CheckboxQuestionProduction, CheckboxesQuestionProduction, EstimateQuestionProduction, Expression, FreetextQuestionProduction, FreetextsQuestionProduction, Name, OptionProduction, RadioQuestionProduction, RadiosQuestionProduction, Sequence, Terminal}
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import edu.umass.cs.automan.core.util.Utilities

object linda_test_survey extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  val pronouns = Map[String, String](
      "Linda" -> "she",
      "Dan" -> "he",
      "Emmie" -> "she",
      "Xavier the bloodsucking spider" -> "it"
    )

    // TODO: what if multiple params need same function?
    val articles = Map[String,String](
      "bank teller" -> "a",
      "almond paste mixer" -> "an",
      "tennis scout" -> "a",
      "lawyer" -> "a",
      "professor" -> "a"
    )

    // The problem statement
    val lindaGrammar = Map[Name, Expression](
      Start -> ref("A"),
      nt("A") -> seq(Array(
        binding(nt("Name")),
        term(" is "),
        binding(nt("Age")),
        term(" years old, single, outspoken, and very bright. "),
        fun(pronouns, nt("Name"), true),
        term(" majored in "),
        binding(nt("Major")),
        term(". As a student, "),
        fun(pronouns, nt("Name"), false),
        term(" was deeply concerned with issues of "),
        binding(nt("Issue")),
        term(", and also participated in "),
        binding(nt("Demonstration")),
        term(" demonstrations.\n Which is more probable?\n"),
        ref("Opt1"),
        term("\n"),
        ref("Opt2")
      )),
      nt("Name") -> ch(Array(
        term("Linda"),
        term("Dan"),
        term("Emmie"),
        term("Xavier the bloodsucking spider")
      )),
      nt("Age") -> ch(Array(
        term("21"),
        term("31"),
        term("41"),
        term("51"),
        term("61")
      )),
      nt("Major") -> ch(Array(
        term("chemistry"),
        term("psychology"),
        term("english literature"),
        term("philosophy"),
        term("women's studies"),
        term("underwater basket weaving")
      )),
      nt("Issue") -> ch(Array(
        term("discrimination and social justice"),
        term("fair wages"),
        term("animal rights"),
        term("white-collar crime"),
        term("unemployed circus workers")
      )),
      nt("Demonstration") -> ch(Array(
        term("anti-nuclear"),
        term("anti-war"),
        term("pro-choice"),
        term("anti-abortion"),
        term("anti-animal testing")
      )),
      nt("Job") -> ch(Array(
        term("bank teller"),
        term("almond paste mixer"),
        term("tennis scout"),
        term("lawyer"),
        term("professor")
      )),
      nt("Movement") -> ch(Array(
        term("feminist"),
        term("anti-plastic water bottle"),
        term("pro-pretzel crisp"),
        term("pro-metal straw"),
        term("environmental justice")
      )),
      nt("Opt1") -> opt(seq(Array(
        binding(nt("Name")),
        term(" is a "),
        binding(nt("Job")),
        term(".")
      ))),
      nt("Opt2") -> opt(seq(Array(
        binding(nt("Name")),
        term(" is a "),
        binding(nt("Job")),
        term(" and is active in the "),
        binding(nt("Movement")),
        term(" movement.")
      )))
    )

  val lindaQuestionProduction: RadioQuestionProduction = RadioQuestionProduction(lindaGrammar)


  def which_survey(): SurveyOutcome = surveyGrammar(
    budget = 12.00,
    questions = List(
      nop => radioGrammar(
        grammar = lindaGrammar,
        question = lindaQuestionProduction,
        depth = 2,
        variant = 5625
      )(nop)
    ),
    initial_worker_timeout_in_s = 30,
    minimum_spawn_policy = UserDefinableSpawnPolicy(200),
    text = "Quick survey"
  )

  def which_question() = radioGrammar(
    budget = 8.00,
    depth = 2,
    grammar = lindaGrammar,
    question = lindaQuestionProduction,
    variant = 5625
  )

  automan(a) {
    val outcome = which_survey() //which_question()
    outcome.answer match {
      case answers: SurveyAnswers =>
        println("The answers are: " + answers)
      case ans: Answer[_] =>
        println("The answers are: " + ans)
      case _ => println("Something went wrong.")
    }
  }
}
