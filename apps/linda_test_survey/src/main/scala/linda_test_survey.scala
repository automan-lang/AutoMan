import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.question.MTSurvey
import edu.umass.cs.automan.core.answer.SurveyAnswers
import edu.umass.cs.automan.core.grammar.{CheckboxQuestionProduction, CheckboxesQuestionProduction, Choices, EstimateQuestionProduction, FreetextQuestionProduction, FreetextsQuestionProduction, Grammar, Name, OptionProduction, RadioQuestionProduction, RadiosQuestionProduction, Sequence, Function, Terminal}
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
    val lindaS = new Sequence(
      List(
        new Name("Name"),
        new Terminal(" is "),
        new Name("Age"),
        new Terminal(" years old, single, outspoken, and very bright. "),
        new Function(pronouns, "Name", true),
        new Terminal(" majored in "),
        new Name("Major"),
        new Terminal(". As a student, "),
        new Function(pronouns, "Name", false),
        new Terminal(" was deeply concerned with issues of "),
        new Name("Issue"),
        new Terminal(", and also participated in "),
        new Name("Demonstration"),
        new Terminal(" demonstrations.\nWhich is more probable?"),
        new Name("a"),
        new Name("b")
      )
    )

    val opt1: OptionProduction =
      new OptionProduction(
        new Sequence(
          List(
            new Name("Name"),
            new Terminal(" is "),
            new Function(articles, "Job", false),
            new Terminal(" "),
            new Name("Job"),
            new Terminal(".")
        ))
      )
    val opt2: OptionProduction =
        new OptionProduction(
          new Sequence(
            List(
              new Name("Name"),
              new Terminal(" is "),
              new Function(articles, "Job", false),
              new Terminal(" "),
              new Name("Job"),
              new Terminal(" and is active in the "),
              new Name("Movement"),
              new Terminal(" movement.")
            ))
        )

    val lindaGrammar = Grammar(// the grammar
      Map(
        "Start" -> new Name("lindaS"),
        "lindaS" -> lindaS,
        "Name" -> new Choices(
          List(
            new Terminal("Linda"),
            new Terminal("Dan"),
            new Terminal("Emmie"),
            new Terminal("Xavier the bloodsucking spider")
          )
        ),
        "Age" -> new Choices(
          List(
            new Terminal("21"),
            new Terminal("31"),
            new Terminal("41"),
            new Terminal("51"),
            new Terminal("61")
          )
        ),
        "Major" -> new Choices(
          List(
            new Terminal("chemistry"),
            new Terminal("psychology"),
            new Terminal("english literature"),
            new Terminal("philosophy"),
            new Terminal("women's studies"),
            new Terminal("underwater basket weaving")
          )
        ),
        "Issue" -> new Choices(
          List(
            new Terminal("discrimination and social justice"),
            new Terminal("fair wages"),
            new Terminal("animal rights"),
            new Terminal("white collar crime"),
            new Terminal("unemployed circus workers")
          )
        ),
        "Demonstration" -> new Choices(
          List(
            new Terminal("anti-nuclear"),
            new Terminal("anti-war"),
            new Terminal("pro-choice"),
            new Terminal("anti-abortion"),
            new Terminal("anti-animal testing")
          )
        ),
        "Job" -> new Choices(
          List(
            new Terminal("bank teller"),
            new Terminal("almond paste mixer"),
            new Terminal("tennis scout"),
            new Terminal("lawyer"),
            new Terminal("professor")
          )
        ),
        "Movement" -> new Choices(
          List(
            new Terminal("feminist"),
            new Terminal("anti-plastic water bottle"),
            new Terminal("pro-pretzel crisp"),
            new Terminal("pro-metal straw"),
            new Terminal("environmental justice")
          )
        ),
        "a" -> opt1,
        "b" -> opt2
      ),
      "Start"
    )

  val lindaProd: RadioQuestionProduction = new RadioQuestionProduction(lindaGrammar, lindaS)


  def which_survey() = surveyGrammar(
    budget = 8.00,
    questions = List(
      nop => radioGrammar(
        grammar = lindaGrammar,
        //minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = lindaProd,
        variant = 5625
      )(nop)
    ),
    //minimum_spawn_policy = UserDefinableSpawnPolicy(5),
    text = "Quick survey"
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
