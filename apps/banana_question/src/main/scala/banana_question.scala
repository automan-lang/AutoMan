import org.automanlang.adapters.mturk.DSL._
import java.util.UUID
import java.net.URL
import javax.imageio._
import bananalib._
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy

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
    "banana" -> "https://s3.amazonaws.com/automan-demo-images/bananas.jpg",
    "apple" -> "https://s3.amazonaws.com/automan-demo-images/apple.jpg",
    "celery" -> "https://s3.amazonaws.com/automan-demo-images/celery.jpg",
    "cucumber" -> "https://s3.amazonaws.com/automan-demo-images/cucumbers.jpg",
    "orange" -> "https://s3.amazonaws.com/automan-demo-images/oranges.jpg"
  )
  
  // download and resize each image
  val scaled = images.map { case (name,url) =>
    val img = ImageIO.read(new URL(url))
    name -> resize(img, 150)
  }

  // store each image in S3
  val s3client = init_s3(opts('key), opts('secret), bucketname)
  val s3_urls = scaled.map{ case (name,img) => name -> getTinyURL(store_in_s3(img, s3client, bucketname)) }
  val options = s3_urls.map { case (name,url) => choice(Symbol(name), name, url) }.toList
  
  automan(a) {
    val outcome = which_one("Which one of these does not belong?", options)
    outcome.answer match {
      case answer: Answer[Symbol] => println("The answer is: " + answer.value)
      case _ => println("An error occurred.")
    }
  }
}