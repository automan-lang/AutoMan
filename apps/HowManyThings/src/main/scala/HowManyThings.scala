import com.amazonaws.services.s3.AmazonS3Client
import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.core.Utilities
import java.awt.image.BufferedImage
import java.io.File

object HowManyThings extends App {
  val opts = Utilities.unsafe_optparse(args, "HowManyThings.jar")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = true
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

  // Search for a bunch of images
  val urls = get_urls(args(0))

  // download each image
  val images = urls.map(download_image(_))

  // resize each image
  val scaled = images.map(resize(_))

  // store each image in S3
  val s3client = init_s3("key", "secret")
  val s3_urls = scaled.map{ i => store_in_s3(i, s3client) }

  // ask humans for answers
  val answers_urls = s3_urls.map { url =>
    (how_many_things(getTinyURL(url.toString)) -> url)
  }

  // print answers
  answers_urls.foreach { case(a,url) =>
    println("url: " + url + ", answer: " + a().value)
  }

  // helper functions
  def getTinyURL(url: String): String = {
    import java.io.{BufferedReader,InputStreamReader}
    import org.apache.http.impl.client.DefaultHttpClient
    import org.apache.http.client.methods.HttpGet

    var outstr: String = ""
    val htclient = new DefaultHttpClient()
    val htget = new HttpGet("http://tinyurl.com/api-create.php?url=" + url)
    val response = htclient.execute(htget)
    val entity = response.getEntity
    if (entity != null) {
      val br = new BufferedReader(new InputStreamReader(entity.getContent))
      var line: String = ""
      do {
        line = br.readLine()
        if (line != null) {
          outstr += line
        }
      } while (line != null)
    }
    outstr
  }

  def download_image(u: String) : BufferedImage = {

    import java.util.UUID
    import java.net.URL
    import org.apache.commons.io.FileUtils
    import javax.imageio.ImageIO

    new File("tmp").mkdir()
    val f = new File("tmp/" + UUID.randomUUID().toString)
    FileUtils.copyURLToFile(new URL(u), f)
    ImageIO.read(f)
  }

  def get_urls(query: String): List[String] = {
    import scala.collection.JavaConverters._
    import gsearch._

    val c: Client = new Client();
    val results: List[Result] = (0 until 1).map { i =>
      c.searchImagesByOffset(query, new java.lang.Integer(i * 8)).asScala.toList
    }.toList.flatten
    results.map { _.getUnescapedUrl }.distinct
  }

  def store_in_s3(si: File, s3: AmazonS3Client) : String = {
    import java.util.Calendar
    import com.amazonaws.services.s3.model.{PutObjectRequest, CannedAccessControlList}

    s3.putObject(new PutObjectRequest("foo", si.getName, si).withCannedAcl(CannedAccessControlList.PublicRead))
    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, 2)
    s3.generatePresignedUrl("foo", si.getName, cal.getTime).toString
  }

  def init_s3(key: String, secret: String) : AmazonS3Client = {
    import com.amazonaws.auth.BasicAWSCredentials

    val awsAccessKey = key;
    val awsSecretKey = secret;
    val c = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
    val s3 = new AmazonS3Client(c)
    s3.createBucket("foo");
    s3
  }

  def resize(i: BufferedImage): File = {
    import org.imgscalr.Scalr
    import java.util.UUID
    import javax.imageio.ImageIO

    val f = new File("tmp/" + UUID.randomUUID().toString + "_scaled.jpg")
    val si = Scalr.resize(i, 1000)
    ImageIO.write(si, "jpg", f)
    f
  }
}
