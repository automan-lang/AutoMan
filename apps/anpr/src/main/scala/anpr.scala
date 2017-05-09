import edu.umass.cs.automan.adapters.mturk.DSL._
import anprlib._

object anpr extends App {
  val opts = my_optparse(args, "anpr.jar")

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

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