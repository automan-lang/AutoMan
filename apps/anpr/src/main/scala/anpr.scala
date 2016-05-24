import edu.umass.cs.automan.adapters.mturk._
import anprlib._
import edu.umass.cs.automan.core.answer._

object anpr extends App {
  val opts = my_optparse(args, "anpr.jar")

  implicit val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  automan(a) {
    // get plate texts from image URLs
    val urls = getURLsFromDisk
    val plate_texts = urls.map { url =>
      (url, plateTxt(url))
    }

    // print out results
    plate_texts.foreach { case (url, outcome) =>
      outcome.answer match {
        case answer: Answer[String] =>
          println(url + ": " + answer.value)
        case _ => ()
      }
    }
  }
}