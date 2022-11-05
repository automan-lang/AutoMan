import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}
import org.automanlang.core.grammar.Expand._
import org.automanlang.core.grammar._
import org.automanlang.core.info.QuestionType

import scala.collection.immutable.ListMap

object GrammarLindaProblem extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a: MTurkAdapter = mturk(
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

  val articles = Map[String, String](
    "bank teller" -> "a",
    "almond paste mixer" -> "an",
    "tennis scout" -> "a",
    "lawyer" -> "a",
    "professor" -> "a"
  )

  val lindaGrammar = Map[Name, Expression](
    Start -> ref("A"),
    nt("A") -> seq(Array(
      binding(nt("Person")),
      term(" is "),
      binding(nt("Age")),
      term(" years old, single, outspoken, and very bright. "),
      fun(pronouns, nt("Person"), capitalize = true),
      term(" majored in "),
      binding(nt("Major")),
      term(". As a student, "),
      fun(pronouns, nt("Person"), capitalize = false),
      term(" was deeply concerned with issues of "),
      binding(nt("Issue")),
      term(", and also participated in "),
      binding(nt("Demonstration")),
      term(" demonstrations.\nWhich is more probable?\n"),
      ref("Opt1"),
      term("\n"),
      ref("Opt2")
    )),
    nt("Person") -> ch(Array(
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
      term("white collar crime"),
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
      binding(nt("Person")),
      term(" is a "),
      binding(nt("Job")),
      term(".")
    ))),
    nt("Opt2") -> opt(seq(Array(
      binding(nt("Person")),
      term(" is a "),
      binding(nt("Job")),
      term(" and is active in the "),
      binding(nt("Movement")),
      term(" movement.")
    )))
  )


  def which_one(): DSL.SurveyOutcome[List[Any]] = GrammarSurvey(
    grammar = List(lindaGrammar),
    questionType = List(QuestionType.RadioButtonQuestion),
    depth = 2,
    budget = 100.00,  // this field is a hard limit per question/survey on how much the user will pay
    // (the survey will terminate if total price of tasks increase beyond this limit and throw OverBudgetException`)
    // TODO: set a different default value/function based on survey
    csv_output = "linda_grammar_" + java.time.LocalDateTime.now.toString + ".csv",
    title = "Which is more probable?",
    text = "",
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
