import edu.umass.cs.automan.adapters.mturk._
import edu.umass.cs.automan.adapters.mturk.question.MTQuestionOption
import java.util.UUID
import java.net.URL
import javax.imageio._
import bananalib._

object banana_question extends App {
  val opts = my_optparse(args, "banana_question.jar")

  val bucketname = ("bananaquestion" + UUID.randomUUID().toString).toLowerCase

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  def which_one(text: String, options: List[MTQuestionOption]) = a.RadioButtonQuestion { q =>
    q.budget = 5.00
    q.text = text
    q.options = options
  }
  
  // images:
  val images = Map(
    "banana" -> "http://tinyurl.com/qej5zad",
    "apple" -> "http://tinyurl.com/qxn58kn",
    "celery" -> "http://tinyurl.com/q38566j",
    "cucumber" -> "http://tinyurl.com/ps6bgg3",
    "orange" -> "http://tinyurl.com/ntxzjjt"
  )
  
  // download and resize each image
  val scaled = images.map { case (name,url) => name -> resize(ImageIO.read(new URL(url)), 150) }

  // store each image in S3
  val s3client = init_s3(opts('key), opts('secret), bucketname)
  val s3_urls = scaled.map{ case (name,img) => name -> getTinyURL(store_in_s3(img, s3client, bucketname)) }
  
  // create options
  val banana_opts = s3_urls.map { case (name, url) => a.Option(Symbol(name), name, url) }.toList

  automan(a) {
    val outcome = which_one("Which one of these does not belong?", banana_opts)
    outcome.answer match {
      case Answer(answer,_,_) => println("The answer is: " + answer)
      case _ => println("An error occurred.")
    }
  }
}