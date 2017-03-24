//import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.DSL._

object CalorieCounter extends App {

  val foodImg = "https://s3.amazonaws.com/edu-umass-cs-automan-2014-07-01-calorie/0715a58400d974009fa79368a05471ac.jpg"
  val opts = Utilities.unsafe_optparse(args, "CalorieCounter")

  implicit val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def howManyCals(imgUrl: String) = estimate (
    budget = 6.00,
    confidence_interval = SymmetricCI(50),
    text = "Estimate how many calories (kcal) are " +
      "present in the picture shown in the photo.",
    image_url = imgUrl,
    min_value = 0
  )

  automan(a) {
    howManyCals(foodImg).answer match {
      case e: Estimate =>
        println("Estimate: " + e.value + ", low: " + e.low +
                ", high: " + e.high + ", cost: $" + e.cost +
                ", confidence: " + e.confidence)
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