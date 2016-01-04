import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.core.answer._

object CalorieCounter extends App {

  val foodImg = "http://s1.1zoom.me/big3/635/343470-svetik.jpg"
  val opts = Utilities.unsafe_optparse(args, "CalorieCounter")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def howManyCals(imgUrl: String) = a.EstimationQuestion { q =>
    q.budget = 6.00
    q.confidence_interval = SymmetricCI(50)
    q.text = "Estimate how many calories (kcal) are " +
             "present in the picture shown in the photo."
    q.image_url = imgUrl
  }

  automan(a) {
    howManyCals(foodImg).answer match {
      case Estimate(est, low, high, cost, conf) =>
        println("Estimate: " + est + ", low: " + low +
                ", high: " + high + ", cost: $" + cost +
                ", confidence: " + conf)
      case LowConfidenceEstimate(est, low, high, cost, conf) =>
        println("Low-Confidence Estimate: " + est +
                ", low: " + low + ", high: " + high +
                ", cost: $" + cost + ", confidence: " + conf)
      case OverBudgetEstimate(need, have) =>
        println("Over budget; could not produce an estimate. Need $" +
                need +"; have $" + have)
    }
  }
}