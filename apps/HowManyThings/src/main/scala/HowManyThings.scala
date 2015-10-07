import edu.umass.cs.automan.adapters.mturk._
import net.ettinsmoor.Bingerator
import java.util.UUID
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

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def how_many_things(photo_url: String) = a.RadioButtonQuestion { q =>
    q.text = "How many " + args(0) + " are in this picture?"
    q.image_url = photo_url
    q.options = List(
      a.Option('zero, "None"),
      a.Option('one, "One"),
      a.Option('more, "More than one")
    )
  }

  // search for a bunch of images
  val results = new Bingerator(opts('bingkey)).SearchImages(args(0)).take(10).toList

  // download each image
  val images = results.flatMap(_.getImage)

  // resize each image
  val scaled = images.map(resize(_))

  // store each image in S3
  val s3client = init_s3(opts('key), opts('secret), bucketname)
  val s3_urls = scaled.map{ i => store_in_s3(i, s3client, bucketname) }

  automan(a) {
	// ask humans for answers
    val answers_urls = s3_urls.map { url =>
      (how_many_things(getTinyURL(url.toString)) -> url)
    }

    // print answers
    answers_urls.foreach { case(outcome,url) =>
      outcome.answer match {
        case Answer(answer,_,_) => println("url: " + url + ", answer: " + answer)
        case _ => ()
      }
    }
  }
}
