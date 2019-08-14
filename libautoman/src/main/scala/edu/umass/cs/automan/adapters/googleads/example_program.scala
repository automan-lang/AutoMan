package edu.umass.cs.automan.adapters.googleads

import edu.umass.cs.automan.adapters.googleads.DSL._

object example_program extends App {

  implicit val ga = gads(production_account_id = 1234567890) // 10-digit number shown in Google Ads as XXX-XXX-XXXX

  def planets() = estimate(
    form_title = "Question 50",
    text = "How many planets are there in the solar system?",
    confidence_interval = SymmetricCI(50),
    budget = 10.00,
    min_value = 0
  )

  automan(ga) {
    planets().answer match {
      case answer: Estimate =>
        println("The answer is: " + answer.value + ". You spent $" + answer.cost)
      case lowconf: LowConfidenceEstimate =>
        println(
          "You ran out of money. The best answer is \"" +
            lowconf.value + "\" with a confidence of " + lowconf.confidence
        )
      case e: OverBudgetEstimate =>
        println("Over budget; could not produce an estimate. Need $" +
          e.need +"; have $" + e.have)
    }
  }
}