import edu.umass.cs.automan.adapters.mturk.DSL._
import java.util.UUID
import java.net.URL
import javax.imageio._
import bananalib._
import edu.umass.cs.automan.core.policy.aggregation.UserDefinableSpawnPolicy

object banana_question extends App {
  val opts = my_optparse(args, "banana_question.jar")

  val bucketname = ("bananaquestion" + UUID.randomUUID().toString).toLowerCase

  implicit val a = mturk (
    access_key_id = opts('key),
    secret_access_key = opts('secret),
    sandbox_mode = opts('sandbox).toBoolean
  )

  def which_one(text: String, options: List[MTQuestionOption]) = radio (
    budget = 5.00,
    text = text,
    options = options,
    minimum_spawn_policy = UserDefinableSpawnPolicy(0)
  )
  
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
  
  automan(a) {
    val outcome = which_one("Which one of these does not belong?", s3_urls.toList)
    outcome.answer match {
      case answer: Answer[Symbol] => println("The answer is: " + answer.value)
      case _ => println("An error occurred.")
    }
  }
}