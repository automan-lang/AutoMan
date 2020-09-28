import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.core.logging.LogLevelDebug
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object SimpleSandboxTesterEstimate extends App {
  val opts = Utilities.unsafe_optparse(args, "SimpleSandboxTester")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean,
    log_verbosity = LogLevelDebug(),
    database_path = "dbarowy_saved_hits"
  )

  def which_one() = estimate (
    budget = 8.00,
    text = "How many jelly beans are in this jar?",
    image_url = "https://cdn.shopify.com/s/files/1/1009/8338/products/2015-10-12-18.42.jpg?v=1445378595",
    confidence_interval = SymmetricCI(50),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  automan(a) {
    which_one().answer match {
      case answer: Estimate =>
        println("The answer is: " + answer.value)
      case e: LowConfidenceEstimate =>
        println("Low-Confidence Estimate: " + e.value +
          ", low: " + e.low + ", high: " + e.high +
          ", cost: $" + e.cost + ", confidence: " + e.confidence)
      case e: OverBudgetEstimate =>
        println("Over budget; could not produce an estimate. Need $" +
          e.need +"; have $" + e.have)

    }
  }
}