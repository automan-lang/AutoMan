import edu.umass.cs.automan.adapters.mturk.DSL._
import net.ettinsmoor.Bingerator
import java.util.UUID
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy
import hmtlib._

// This application counts the number of items in a set of images
// where the item type is an arbitary type specified by the user.
// Images are fetched from Bing Image Search using the keyword
// given by the user.
// Call this program with no arguments for usage information.
object HowManyThings extends App {
  // random bucket name
  val bucketname = ("howmanythings" + UUID.randomUUID().toString).toLowerCase

  // read configuration
  val opts = config(args)

  implicit val mt = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def how_many_things(photo_url: String) = radio (
    text = "How many " + args(0) + " are in this picture?",
    image_url = photo_url,
    options = (
      "None",
      "One",
      "More than one"
    ),
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )

  // search for a bunch of images
  val results = new Bingerator(opts('bingkey)).SearchImages(args(0)).take(10).toList

  // download each image
  val images = results.flatMap(_.getImage)

  // resize each image
  val scaled = images.map(resize(_))

  // store each image in S3
  val s3client = init_s3(opts('key), opts('secret), bucketname)
  val s3_urls = scaled.map { i => store_in_s3(i, s3client, bucketname) }

  automan(mt) {
	  // ask humans for answers
    val answers_urls = s3_urls.map { url =>
      how_many_things(getTinyURL(url.toString)) -> url
    }

    // print answers
    answers_urls.foreach { case(outcome,url) =>
      outcome.answer match {
        case a:Answer[Symbol] => println("url: " + url + ", answer: " + a.value)
        case _ => ()
      }
    }
  }
}
