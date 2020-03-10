import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.question.MTSurvey
import edu.umass.cs.automan.core.answer.SurveyAnswers
import edu.umass.cs.automan.core.grammar.{CheckboxQuestionProduction, CheckboxesQuestionProduction, Choices, EstimateQuestionProduction, FreetextQuestionProduction, FreetextsQuestionProduction, Grammar, Name, OptionProduction, RadioQuestionProduction, RadiosQuestionProduction, Sequence, Terminal}
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import edu.umass.cs.automan.core.util.Utilities

object simple_survey extends App {
  val opts = Utilities.unsafe_optparse(args, "simple_program")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  val cbSeq = new Sequence(
    List(
      new Terminal("How much does "),
      new Name("Object"),
      new Terminal(" weigh?"),
      new Name("a"),
      new Name("b"),
      new Name("c")
    )
  )

  val eSeq = new Sequence(
    List(
      new Terminal("How much does "),
      new Name("Object"),
      new Terminal(" weigh?")
    )
  )

  val optList: List[OptionProduction] = List(
    new OptionProduction(new Terminal("1 lb")),
    new OptionProduction(new Terminal("1,000 lb")),
    new OptionProduction(new Terminal("10,000 lb")) // TODO test with vars in option
  )

  val cbGram: Grammar = Grammar(
    Map(
      "Start" -> new Name("Seq"),
      "Seq" -> cbSeq,
      "Object" -> new Choices(
        List(
          new Terminal("an ox"),
          new Terminal("an ocarina"),
          new Terminal("an obelisk")
        )
      ),
      "a" -> new OptionProduction(new Terminal("1 lb")),//optASeq),
      "b" -> new OptionProduction(new Terminal("1,000 lb")),//optBSeq),
      "c" -> new OptionProduction(new Terminal("10,000 lb"))
      //"Options" -> optSeq // we need a name here
    ),
    "Start"
  )

  val estGram: Grammar = Grammar(
    Map(
      "Start" -> new Name("Seq"),
      "Seq" -> eSeq,
      "Object" -> new Choices(
        List(
          new Terminal("an ox"),
          new Terminal("an ocarina"),
          new Terminal("an obelisk")
        )
      )
    ),
    "Start"
  )
  val estProd: EstimateQuestionProduction = new EstimateQuestionProduction(estGram, eSeq) // todo verify estimation Qs don't have opts
  val cbProd: CheckboxQuestionProduction = new CheckboxQuestionProduction(cbGram, cbSeq) // todo is opts necessary? may have made totext method too complicated
  val cbsProd: CheckboxesQuestionProduction = new CheckboxesQuestionProduction(cbGram, cbSeq)
  val ftProd: FreetextQuestionProduction = new FreetextQuestionProduction(estGram, eSeq)
  val rbProd: RadioQuestionProduction = new RadioQuestionProduction(cbGram, cbSeq)
  val rbsProd: RadiosQuestionProduction = new RadiosQuestionProduction(cbGram, cbSeq)
//  def which_grammar() = checkboxGrammar (
//    grammar = estGrammar,
//    minimum_spawn_policy = UserDefinableSpawnPolicy(0),
//    question = cbProd
//  )

  def which_grammar() = checkboxGrammar(
    grammar = cbGram,
    minimum_spawn_policy = UserDefinableSpawnPolicy(0),
    question = cbProd, //estProd
    variant = 0
  )

  def which_gram_survey() = surveyGrammar(
    budget = 8.00,
    questions = List(
      nop => checkboxGrammar (
        grammar = cbGram,
        minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = cbProd, //estProd
        variant = 1
      )(nop),
      nop => checkboxesGrammar (
        grammar = cbGram,
        minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = cbsProd, //estProd
        variant = 1
      )(nop),
      nop => radioGrammar (
        grammar = cbGram,
        minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = rbProd, //estProd
        variant = 2
      )(nop),
      nop => radiosGrammar (
        grammar = cbGram,
        minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = rbsProd, //estProd
        variant = 3
      )(nop),
      nop => freetextGrammar (
        grammar = estGram,
        minimum_spawn_policy = UserDefinableSpawnPolicy(0),
        question = ftProd,
        pattern = "[0-9]*",
        variant = 0
      )(nop)
    ),
    text = "Sample survey"
  )

  def which_survey() = survey (
    budget = 8.00,
    questions = List(
      nop => checkbox (
        budget = 8.00,
        text = "Which one of these does not belong?",
        options = (
          choice('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
          choice('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
          choice('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
          choice('cookiemonster, "Cookie Monster", "http://tinyurl.com/otb6thl"),
          choice('thecount, "The Count", "http://tinyurl.com/nfdbyxa")
        ),
        minimum_spawn_policy = UserDefinableSpawnPolicy(0)
      )(nop),
      nop => radio (
        budget = 8.00,
        text = "Which one of these is a frog?",
        options = (
          choice('oscar, "Oscar the Grouch", "http://tinyurl.com/qfwlx56"),
          choice('kermit, "Kermit the Frog", "http://tinyurl.com/nuwyz3u"),
          choice('spongebob, "Spongebob Squarepants", "http://tinyurl.com/oj6wzx6"),
          choice('cookiemonster, "Cookie Monster", "http://tinyurl.com/otb6thl"),
          choice('thecount, "The Count", "http://tinyurl.com/nfdbyxa")
        ),
        minimum_spawn_policy = UserDefinableSpawnPolicy(0)
      )(nop)
    ),
    text = "foo"
  )

  automan(a) {
    val outcome = which_gram_survey() //which_survey()
        outcome.answer match {
//          case est: Estimate =>
//            println("The answers are: " + est.value)
          case answers: SurveyAnswers =>
            //val ansMap = answers.values
            println("The answers are: " + answers)// + answers.values)
            //for(v <- ansMap) println(v.question)
          case ans: Answer[_] =>
            println("The answers are: " + ans)
          case _ => println("Something went wrong.")
        }
      }
  }
//    val outcome = which_survey()
//    outcome.answer match {
//     case answers: SurveyAnswers =>
//        println("The answers are: " + answers.values) // TODO: this is printing weirdly but it does it right in the scheduler output when it calls prettyPrint
//      case _ => println("Something went wrong.")
//    }
//  }
//which_survey().survey.asInstanceOf[MTSurvey].prettyPrintAnswer(answers.values)