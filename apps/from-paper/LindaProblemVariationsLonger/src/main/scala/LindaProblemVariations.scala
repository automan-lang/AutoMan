import org.automanlang.adapters.mturk.DSL._
import org.automanlang.adapters.mturk.{DSL, MTurkAdapter}
import scala.collection.immutable.ListMap

object LindaProblemVariations extends App {
  val opts = Utilities.unsafe_optparse(args, "LindaProblemVariations")
  val TRIALS = 5

  implicit val a: MTurkAdapter = mturk(
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def ktQuestions(n: Int) = {
    (for (i <- 0 until n) yield
      radioQuestion(
        text = "${description" + i + "}<br><br>Which is more probable?",
        options = (
          choice('A, "${name" + i + "} is a ${profession" + i + "}"),
          choice('B, "${name" + i + "} is a ${profession" + i + "} and ${attribute" + i + "}")
        )
      )
    ).toList
  }

  def ktFunctions(n: Int):  ListMap[String, (String, ListMap[String, String])] = {
    val list_of_maps = {
      (for (i <- 0 until n) yield {
        val i_str = i.toString
        val ktMap =
          ListMap(
            "pronoun" + i_str -> ("name" + i_str, ListMap(
              "Liam" -> "he", "Noah" -> "he", "Oliver" -> "he", "Elijah" -> "he", "James" -> "he",
              "Olivia" -> "she", "Emma" -> "she", "Charlotte" -> "she", "Amelia" -> "she", "Ava" -> "she"
            )),
            "possessive" + i_str -> ("name" + i_str, ListMap(
              "Liam" -> "his", "Noah" -> "his", "Oliver" -> "his", "Elijah" -> "his", "James" -> "his",
              "Olivia" -> "her", "Emma" -> "her", "Charlotte" -> "her", "Amelia" -> "her", "Ava" -> "her"
            )),
            "determiner" + i_str -> ("name" + i_str, ListMap(
              "Liam" -> "him", "Noah" -> "him", "Oliver" -> "him", "Elijah" -> "him", "James" -> "him",
              "Olivia" -> "her", "Emma" -> "her", "Charlotte" -> "her", "Amelia" -> "her", "Ava" -> "her"
            )),
            "attribute" + i_str -> ("sketch" + i_str, ListMap(
              "absent-minded professor" -> "often publicly points out other people's mistakes",
              "annoying neighbor" -> ("is opposed to efforts to build high density housing in ${possessive" + i_str + "} neighborhood"),
              "curmudgeon" -> "wants to make America great again",
              "hipster" -> "seems to care more about buying a good latte than anything else in life",
              "gung-ho" -> "actively supports the National Rifle Association",
              "romantic" -> "volunteers at the local animal shelter",
              "jock" -> "doesn't care or think about much",
              "nerd" -> "has many ambitions which go unrealized due to a lack of self-confidence",
              "reluctant hero" -> "does not want to get in a fight but won't back down when challenged",
              "town drunk" -> ("despite appearances, would do anything for ${possessive" + i_str + "} daughter"),
            )),
            "description" + i_str -> ("sketch" + i_str, ListMap(
              "absent-minded professor" -> ("${name" + i_str + "} is an eccentric genius; ${pronoun" + i_str + "} is very focused on ${possessive" + i_str + "} work, but tends to forget to do ordinary things like combing ${possessive" + i_str + "} hair or saying hello to ${possessive" + i_str + "} neighbors."),
              "annoying neighbor" -> ("${name" + i_str + "} spends a lot of time working in ${possessive" + i_str + "} garden; ${pronoun" + i_str + "} also spends a lot of time criticizing the state of ${possessive" + i_str + "} neighbor's houses, and many of ${name" + i_str + "}'s neighbors do not like ${determiner" + i_str + "}."),
              "curmudgeon" -> ("${name" + i_str + "} is an elderly resident who spends much of ${possessive" + i_str + "} time at the local donut shop arguing with other patrons about politics. ${name" + i_str + "} sometimes makes inappropriate remarks to young patrons of the shop, much to the chagrin of the shop owners who let ${determiner" + i_str + "} stay around because ${pronoun" + i_str + "} is a good customer."),
              "hipster" -> ("${name" + i_str + "} is in ${possessive" + i_str + "} mid-30s, likes to wear jeans and flannel, and has several visible arm tattoos; ${pronoun" + i_str + "} studied psychology in college. ${name" + i_str + "} spends most of ${possessive" + i_str + "} free time at the local bar after work."),
              "gung-ho" -> ("${name" + i_str + "} was formerly in the military, and is now in ${possessive" + i_str + "} mid-forties with two kids and a spouse; ${pronoun" + i_str + "} owns a big truck and likes to go hunting on the weekends with ${possessive" + i_str + "} kids. ${name" + i_str + "} thinks that the country is on the wrong track."),
              "romantic" ->("${name" + i_str + "} reliably goes to work everyday, but has few good friends and is lonely; ${pronoun" + i_str + "} takes a lot of comfort from watching romantic comedies with ${possessive" + i_str + "} cat, Pickles. ${name" + i_str + "} sometimes daydreams about dating coworkers or neighbors, but never has the courage to talk to any of them."),
              "jock" -> ("${name" + i_str + "} runs every morning and goes to the gym after work to lift weights. Due to ${possessive" + i_str + "} physique, ${name" + i_str + "} is admired by members of the opposite sex and goes on a lot of dates; ${pronoun" + i_str + "} has not read a book in years."),
              "nerd" -> ("${name" + i_str + "} is socially awkward and overly-intellectual. Despite being in ${possessive" + i_str + "} mid-twenties, ${name" + i_str + "} still wears clothes bought by ${possessive" + i_str + "} mom. ${name" + i_str + "} is obsessed with computer programming and playing board games."),
              "reluctant hero" -> ("${name" + i_str + "} is a former police officer who quit due to corruption in the mayor's office. ${name" + i_str + "} cares a lot about the safety of ${possessive" + i_str + "} family; ${pronoun" + i_str + "} spends the weekends coaching ${possessive" + i_str + "} daughter's softball team."),
              "town drunk" -> ("${name" + i_str + "} struggles with drinking, but mostly manages to hold it together; ${pronoun" + i_str + "} lives in a modest house, alone, having alienated ${possessive" + i_str + "} family. ${name" + i_str + "} manages to stay sober whenever ${possessive" + i_str + "} young daughter comes to visit."),
            )),
          )
        ktMap
      }).toList
    }
    val d = scala.collection.mutable.LinkedHashMap[String, (String, ListMap[String, String])]()
    for (map <- list_of_maps) {
      for (kvp <- map) {
        val (key, value) = kvp
        // look for the outer key
        if (!d.contains(key)) {
          // if it does not exist, copy the entire tuple
          d.put(key, value)
        } else {
          // should be impossible
          throw new Exception(s"Duplicate key '${key}'!")
        }
      }
    }
    // convert to immutable listmap
    val immutableListMap = ListMap[String, (String, ListMap[String, String])](d.toSeq: _*)
    immutableListMap
  }

  def ktWords(n: Int): ListMap[String, Array[String]] = {
    val list_of_maps = {
      (for (i <- 0 until n) yield {
        val i_str = i.toString
        val ktMap =
          ListMap[String, Array[String]](
            "name" + i_str -> Array("Liam", "Olivia", "Noah", "Emma", "Oliver", "Charlotte", "Elijah", "Amelia", "James", "Ava"),
            "profession" + i_str -> Array("a teacher", "a bartender", "a police officer", "an electrician", "a carpenter", "a software developer", "a lawyer", "a doctor", "a server", "a janitor", "a farmer", "a photographer"),
            "sketch" + i_str -> Array("absent-minded professor", "annoying neighbor", "curmudgeon", "hipster", "gung-ho", "romantic", "jock", "nerd", "reluctant hero", "town drunk")
          )
        ktMap
      }).toList
    }
    val d = scala.collection.mutable.LinkedHashMap[String, Array[String]]()
    for (map <- list_of_maps) {
      for (kvp <- map) {
        val (key, xs) = kvp
        if (!d.contains(key)) {
          d.put(key, xs)
        } else {
          // should be impossible
          throw new Exception(s"Duplicate key '${key}'!")
        }
      }
    }
    // convert to immutable listmap
    val immutableListMap = ListMap[String, Array[String]](d.toSeq: _*)
    immutableListMap
  }

  def which_one(): DSL.SurveyOutcome[List[Any]] = Survey(
    questions = ktQuestions(TRIALS),
    functions = ktFunctions(TRIALS),
    words_candidates = ktWords(TRIALS),
    budget = 100.00 * TRIALS,  // this field is a hard limit per question/survey on how much the user will pay
    // (the survey will terminate if total price of tasks increase beyond this limit and throw OverBudgetException`)
    csv_output = "linda_variation_" + java.time.LocalDateTime.now.toString + ".csv",
    title = "Which is more probable?",
    text = "This survey contains a set of short descriptions and questions.  " +
           "Please read the description and answer the question immediately following it " +
           "before proceeding to the next description.",
    sample_size = 200,
    initial_worker_timeout_in_s = 60 * TRIALS,
    question_timeout_multiplier = 180,  // used to calculate the time of an epoch determining "TIMEOUT" sate
    noise_percentage = 0.4,
    cohen_d_threshold = -1000,
    wage = 7.25 / 2
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
