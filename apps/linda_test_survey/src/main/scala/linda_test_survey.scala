import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.question.MTSurvey
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{SurveyAnswers, SurveyOutcome}
import edu.umass.cs.automan.core.grammar.Expand.{Start, binding, ch, fun, nt, opt, ref, seq, term}
import edu.umass.cs.automan.core.grammar.Rank.Grammar
import edu.umass.cs.automan.core.grammar.{CheckboxQuestionProduction, CheckboxesQuestionProduction, EstimateQuestionProduction, Expression, FreetextQuestionProduction, FreetextsQuestionProduction, Name, OptionProduction, QuestionProduction, RadioQuestionProduction, RadiosQuestionProduction, Sequence, Terminal}
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import edu.umass.cs.automan.core.question.Dimension
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

    val articles = Map[String,String](
      "bank teller" -> "a",
      "almond paste mixer" -> "an",
      "tennis scout" -> "a",
      "lawyer" -> "a",
      "professor" -> "a"
    )

    // The problem statement
//    val lindaS = new Sequence(
//      List(
//        new Name("Name"),
//        new Terminal(" is "),
//        new Name("Age"),
//        new Terminal(" years old, single, outspoken, and very bright. "),
//        new Function(pronouns, "Name", true),
//        new Terminal(" majored in "),
//        new Name("Major"),
//        new Terminal(". As a student, "),
//        new Function(pronouns, "Name", false),
//        new Terminal(" was deeply concerned with issues of "),
//        new Name("Issue"),
//        new Terminal(", and also participated in "),
//        new Name("Demonstration"),
//        new Terminal(" demonstrations.\nWhich is more probable?"),
//        new Name("a"),
//        new Name("b")
//      )
//    )
//
//    val opt1: OptionProduction =
//      new OptionProduction(
//        new Sequence(
//          List(
//            new Name("Name"),
//            new Terminal(" is "),
//            new Function(articles, "Job", false),
//            new Terminal(" "),
//            new Name("Job"),
//            new Terminal(".")
//        ))
//      )
//    val opt2: OptionProduction =
//        new OptionProduction(
//          new Sequence(
//            List(
//              new Name("Name"),
//              new Terminal(" is "),
//              new Function(articles, "Job", false),
//              new Terminal(" "),
//              new Name("Job"),
//              new Terminal(" and is active in the "),
//              new Name("Movement"),
//              new Terminal(" movement.")
//            ))
//        )

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
      term(" demonstrations.\nWhich is more probable?\n"),
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

  val lindaQuestionProduction: RadioQuestionProduction = new RadioQuestionProduction(lindaGrammar)
  val lindaProd1: RadioQuestionProduction = new RadioQuestionProduction(lindaGrammar)


//  def which_survey() = surveyGrammar(
//    budget = 12.00,
//    questions = List(
//      nop => radioGrammar(
//        grammar = lindaG,
//        question = lindaProd,
//        depth = 2,
//        variant = 5625
//      )(nop)
//    ),
//    initial_worker_timeout_in_s = 30,
//    minimum_spawn_policy = UserDefinableSpawnPolicy(2),
//    text = "Quick survey"
//  )
/**
  def which_survey() = surveyGrammar(
    budget = 8.00,
    questions = List(
//      nop => multiestimate(
//          //dimension = Dimension('dim, UnconstrainedCI(), None, None, )
//      )
      nop => radioGrammar(
        grammar = lindaG,
        //minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = lindaProd,
        variant = 435245353,
        depth = 2
      )(nop),
      nop => radioGrammar(
        grammar = lindaG,
        //minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = lindaProd1,
        variant = 0,
        depth = 2
      )(nop)
//      ,
//      nop => radio(
//        text = "test",
//        options = (
//          choice('a,"a"),
//          choice('b,"b"),
//          choice('c,"c")
//        )
//      )(nop)
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(1),
    text = "Quick survey",
    initial_worker_timeout_in_s = 900
  )
*/
  def which_radio() = radioGrammar(
    grammar = lindaGrammar,
    minimum_spawn_policy = UserDefinableSpawnPolicy(0),
    question = lindaQuestionProduction,
    variant = 435245353,
    depth = 2
  )

  def radioG(grammar: Grammar, question: QuestionProduction, depth: Int, variant: Int) = {
    nop: AutomanAdapter => radioGrammar(
      grammar = grammar,
      question = question,
      depth = depth,
      variant = variant
    )(nop)
  }

  def which_survey(): SurveyOutcome = surveyGrammar(
    budget = 12.00,
    questions = List(
      radioG(
        grammar = lindaGrammar,
        question = lindaQuestionProduction,
        depth = 2,
        variant = 5625
      )
    ),
    initial_worker_timeout_in_s = 30,
    text = "Quick survey"
  )


  def how_many_things() = radio (
    text = "How many things are in this picture?",
    options = (
      choice('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
      choice('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
      choice('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
      choice('cookiemonster, "Cookie Monster", "http://tinyurl.com/otb6thl"),
      choice('thecount, "The Count", "http://tinyurl.com/nfdbyxa")
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
//    val outcome = how_many_things()
//    outcome.answer match {
//      case a:Answer[Symbol] => println("answer: " + a.value)
//      case _ => ()
//    }
    val outcome = which_survey()//which_radio() //which_survey()
    outcome.answer match {//outcome.answer match {
      case answers: SurveyAnswers =>
        //answers.toCSV
        println("The answers are: " + answers)// + answers.values)
      case ans: Answer[_] =>
        println("The answers are: " + ans)
      case _ => println("Something went wrong when returning the answer.")
    }
  }
}
