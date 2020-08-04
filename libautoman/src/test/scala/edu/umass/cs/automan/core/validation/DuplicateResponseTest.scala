package edu.umass.cs.automan.core.validation

import edu.umass.cs.automan.core.answer.Estimate
import edu.umass.cs.automan.core.logging.LogLevelDebug
import org.scalatest._
import java.util.UUID
import edu.umass.cs.automan.test._
import edu.umass.cs.automan.adapters.mturk.DSL._
import edu.umass.cs.automan.adapters.mturk.mock.MockSetup

class DuplicateResponseTest extends FlatSpec with Matchers {

  "An estimation program" should "correctly eliminate duplicate responses" in {
    val confidence = 0.95
    val ci = SymmetricCI(50)

    implicit val mt = mturk (
      access_key_id = UUID.randomUUID().toString,
      secret_access_key = UUID.randomUUID().toString,
      use_mock = MockSetup(balance = 8.00),
      logging = LogConfig.NO_LOGGING,
      log_verbosity = LogLevelDebug()
    )

    automan(mt, test_mode = true) {
      def jellyBeanCount() = estimate (
        confidence = confidence,
        budget = 8.00,
        text = "How many jelly beans are in this jar?",
        confidence_interval = ci,
        // from R using rnorm(500, mean = 633, sd = 100)
        // mean = 623.87
        // n = 500
        // sd = 99.1885
        mock_answers = makeDuplicateMocks(1.0/3.0,630,656,598,586,607,
          629,715,632,645,670,
          444,511,568,582,634,690,733,502,623,447,688,650,728,560,529,679,
          829,607,436,524,583,727,571,719,747,655,660,642,519,534,530,743,
          712,764,703,658,660,598,521,533,609,547,742,696,796,489,471,593,
          701,653,496,666,595,673,573,726,703,607,718,550,654,635,625,394,
          571,722,517,727,494,583,472,510,518,510,713,652,412,656,625,658,
          666,406,628,754,606,605,569,717,619,984,635,441,702,519,601,483,
          630,664,659,525,436,586,435,632,651,507,440,570,631,547,517,734,
          513,608,688,553,691,825,775,590,530,650,671,696,595,733,680,644,
          644,706,682,730,711,626,673,501,582,608,611,729,836,707,685,616,
          580,882,429,696,656,485,703,452,512,633,775,700,688,577,873,535,
          587,606,681,655,491,780,666,749,667,560,622,692,662,714,650,686,
          559,708,642,600,655,560,554,549,604,557,649,556,795,557,757,729,
          593,598,650,633,751,617,610,676,559,625,526,459,634,455,643,793,
          571,711,380,607,718,515,590,668,505,564,531,494,653,646,546,498,
          602,739,573,644,672,642,632,585,581,623,674,809,729,656,687,658,
          612,541,864,738,643,506,493,680,595,653,761,747,657,715,548,591,
          486,735,587,646,609,682,739,854,783,588,660,744,568,574,720,774,
          706,608,583,525,540,603,535,624,720,468,672,734,732,615,441,590,
          480,691,523,684,500,655,564,446,532,780,632,543,683,754,494,640,
          690,445,578,672,701,687,658,487,694,565,736,494,703,746,729,490,
          770,648,755,723,550,524,510,592,722,473,401,728,574,742,592,717,
          705,803,538,582,661,739,633,535,443,661,418,776,590,632,570,482,
          458,982,694,664,839,667,606,777,484,642,699,579,777,537,712,579,
          641,792,845,726,681,606,537,624,470,589,653,558,690,699,635,455,
          553,536,532,772,577,554,561,652,444,658,453,711,767,677,671,604,
          463,714,756,670,572,491,392,608,433,482,533,567,494,610,782,597,
          735,700,481,665,578,556,681,699,765,637,634,512,682,438,684,635,
          609,568,539,711,537,638,538,541,572,588,607,513,706,621,450,566,
          321,541,715,624,600,788,647,647,700,624,632,642,675,552,681,577,
          669,512,562,487,635,783,740,659,671,559,639,764,668,684,512,587,
          603,696,610,727,788,611,582,636,650,761)
      )

      jellyBeanCount().answer match {
        case Estimate(est, low, high, cost, conf, _, _) =>
          println("Estimate: " + est + ", low: " + low + ", high: " + high + ", cost: $" + cost + ", confidence: " + conf)
          (est - low <= ci.error) should be (true)
          (high - est <= ci.error) should be (true)
          (est > low && est < high) should be (true)
          (conf >= confidence) should be (true)
          (cost >= BigDecimal(0.48)) should be (true)
        case _ =>
          fail()
      }
    }
  }
}
