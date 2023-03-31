import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}

import scala.collection.immutable.ListMap

object LindaProblemVariations extends App {
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
          // choice('A, "${name} is ${profession}"),
          choice('A, "${name} is ${profession} and may have other interests."),
          choice('B, "${name} is ${profession} and ${attribute}."),
        )
      ),
    ),
    budget = 100.00,  // this field is a hard limit per question/survey on how much the user will pay
    // (the survey will terminate if total price of tasks increase beyond this limit and throw OverBudgetException`)
    csv_output = "linda_variation_" + java.time.LocalDateTime.now.toString + ".csv",
    title = "Which is more probable?",
    text = "${description}",
    words_candidates = ListMap[String, Array[String]](
      "name" -> Array("Liam", "Olivia", "Noah", "Emma", "Oliver", "Charlotte", "Elijah", "Amelia", "James", "Ava"),
      "profession" -> Array("a teacher", "a bartender", "a police officer", "an electrician", "a carpenter", "a software developer", "a lawyer", "a doctor", "a server", "a janitor", "a farmer", "a photographer"),
      "sketch" -> Array("absent-minded professor", "annoying neighbor", "curmudgeon", "hipster", "gung-ho", "romantic", "jock", "nerd", "reluctant hero", "town drunk")
    ),
    functions = ListMap(
      "pronoun" -> ("name", Map(
        "Liam" -> "he", "Noah" -> "he", "Oliver" -> "he", "Elijah" -> "he", "James" -> "he",
        "Olivia" -> "she", "Emma" -> "she", "Charlotte" -> "she", "Amelia" -> "she", "Ava" -> "she"
      )),
      "possessive" -> ("name", Map(
        "Liam" -> "his", "Noah" -> "his", "Oliver" -> "his", "Elijah" -> "his", "James" -> "his",
        "Olivia" -> "her", "Emma" -> "her", "Charlotte" -> "her", "Amelia" -> "her", "Ava" -> "her"
      )),
      "determiner" -> ("name", Map(
        "Liam" -> "him", "Noah" -> "him", "Oliver" -> "him", "Elijah" -> "him", "James" -> "him",
        "Olivia" -> "her", "Emma" -> "her", "Charlotte" -> "her", "Amelia" -> "her", "Ava" -> "her"
      )),
      "attribute" -> ("sketch", Map(
        "absent-minded professor"->"often publicly points out other people's mistakes",
        "annoying neighbor"->"is opposed to efforts to build high density housing in ${possessive} neighborhood",
        "curmudgeon"->"wants to make America great again",
        "hipster"->"seems to care more about buying a good latte than anything else in life",
        "gung-ho"->"actively supports the National Rifle Association",
        "romantic"->"volunteers at the local animal shelter",
        "jock"->"doesn't care or think about much",
        "nerd"->"has many ambitions which go unrealized due to a lack of self-confidence",
        "reluctant hero"->"does not want to get in a fight but won't back down when challenged",
        "town drunk"->"despite appearances, would do anything for ${possessive} daughter",
      )),
      "description" -> ("sketch", Map(
        "absent-minded professor"->"${name} is an eccentric genius; ${pronoun} is very focused on ${possessive} work, but tends to forget to do ordinary things like combing ${possessive} hair or saying hello to ${possessive} neighbors.",
        "annoying neighbor"->"${name} spends a lot of time working in ${possessive} garden; ${pronoun} also spends a lot of time criticizing the state of ${possessive} neighbor's houses, and many of ${name}'s neighbors do not like ${determiner}.",
        "curmudgeon"->"${name} is an elderly resident who spends much of ${possessive} time at the local donut shop arguing with other patrons about politics. ${name} sometimes makes inappropriate remarks to young patrons of the shop, much to the chagrin of the shop owners who let ${determiner} stay around because ${pronoun} is a good customer.",
        "hipster"->"${name} is in ${possessive} mid-30s, likes to wear jeans and flannel, and has several visible arm tattoos; ${pronoun} studied psychology in college. ${name} spends most of ${possessive} free time at the local bar after work.",
        "gung-ho"->"${name} was formerly in the military, and is now in ${possessive} mid-forties with two kids and a spouse; ${pronoun} owns a big truck and likes to go hunting on the weekends with ${possessive} kids. ${name} thinks that the country is on the wrong track.",
        "romantic"->"${name} reliably goes to work everyday, but has few good friends and is lonely; ${pronoun} takes a lot of comfort from watching romantic comedies with ${possessive} cat, Pickles. ${name} sometimes daydreams about dating coworkers or neighbors, but never has the courage to talk to any of them.",
        "jock"->"${name} runs every morning and goes to the gym after work to lift weights. Due to ${possessive} physique, ${name} is admired by members of the opposite sex and goes on a lot of dates; ${pronoun} has not read a book in years.",
        "nerd"->"${name} is socially awkward and overly-intellectual. Despite being in ${possessive} mid-twenties, ${name} still wears clothes bought by ${possessive} mom. ${name} is obsessed with computer programming and playing board games.",
        "reluctant hero"->"${name} is a former police officer who quit due to corruption in the mayor's office. ${name} cares a lot about the safety of ${possessive} family; ${pronoun} spends the weekends coaching ${possessive} daughter's softball team.",
        "town drunk"->"${name} struggles with drinking, but mostly manages to hold it together; ${pronoun} lives in a modest house, alone, having alienated ${possessive} family. ${name} manages to stay sober whenever ${possessive} young daughter comes to visit.",
      )),
    ),
    sample_size = 100,
    initial_worker_timeout_in_s = 60,
    wage=7.25,
    question_timeout_multiplier = 180,  // used to calculate the time of an epoch determining "TIMEOUT" sate
    noise_percentage = 0.4,
    cohen_d_threshold = 0
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
