package edu.umass.cs.automan.adapters.googleads

import java.io.File

import com.google.api.client.auth.oauth2.StoredCredential

import scala.collection.JavaConverters._
import com.google.api.client.util.store.{DataStore, FileDataStoreFactory}
import edu.umass.cs.automan.adapters.googleads.DSL._
import edu.umass.cs.automan.core.logging.LogLevelInfo
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy


object concurrent_example extends App {

  val opts = Utilities.unsafe_optparse(args, "concurrent_example")

  implicit val a: GoogleAdsAdapter = gads(
    opts('key).toLong,
    dry_run = false,
    logging = LogConfig.NO_LOGGING,
    log_verbosity = LogLevelInfo()
  )

  def which_one(cpc: BigDecimal) = radio(
    budget = 2.00,
    text = s"Which ingredient goes in coffee?",
    options = (
      choice('milk, "Milk"),
      choice('lemon, "Lemon"),
      choice('salt, "Salt"),
      choice('honey, "Honey")
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0),
    form_title = "Question 14",
    cpc = cpc
  )

  automan(a) {

    val radioOutcomes: List[DSL.ScalarOutcome[Symbol]] =
      List(0.06, 0.12, 0.24, 0.48).map(
        x => which_one(x)
      )

    radioOutcomes foreach { x =>
      x.answer match {
        case answer: Answer[Symbol] =>
          println("CPC = " + answer.question.wage / 120 + ": The answer is " + answer.value + ". You spent $" + answer.cost)
        case lowconf: LowConfidenceAnswer[Symbol] =>
          println(
            lowconf.question.wage / 120 + ": You ran out of money. The best answer is \"" +
              lowconf.value + "\" with a confidence of " + lowconf.confidence
          )
      }
    }

    a.close()
  }
}
