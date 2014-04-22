import com.amazonaws.services.s3.AmazonS3Client
import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.core.Utilities
import java.awt.image.BufferedImage
import java.io.File
import scala.concurrent._
import scala.concurrent.duration._
import java.util.UUID

object HowManyThings extends App {
	// random bucket name
	val bucketname = UUID.randomUUID().toString
	
	// read configuration
	val opts = loadProps()

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

  // Search for a bunch of images
	val results = new Bingerator(opts('key)).SearchImages(args(0))

  // download each image
  val images = results.map(_.getImage())

  // resize each image
  val scaled = images.map(resize(_))

  // store each image in S3
  val s3client = init_s3(opts('key), opts('secret))
  val s3_urls = scaled.map{ i => store_in_s3(i, s3client) }

  // ask humans for answers
  val answers_urls = s3_urls.map { url =>
    (how_many_things(getTinyURL(url.toString)) -> url)
  }

  // print answers
  answers_urls.foreach { case(f,url) =>
	  val a = Await.result(f, Duration.Inf)
    println("url: " + url + ", answer: " + a.value)
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

  def store_in_s3(si: File, s3: AmazonS3Client) : String = {
    import java.util.Calendar
    import com.amazonaws.services.s3.model.{PutObjectRequest, CannedAccessControlList}

    s3.putObject(new PutObjectRequest(bucketname, si.getName, si).withCannedAcl(CannedAccessControlList.PublicRead))
    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, 2)
    s3.generatePresignedUrl(bucketname, si.getName, cal.getTime).toString
  }

  def init_s3(key: String, secret: String) : AmazonS3Client = {
    import com.amazonaws.auth.BasicAWSCredentials

    val awsAccessKey = key;
    val awsSecretKey = secret;
    val c = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
    val s3 = new AmazonS3Client(c)
    s3.createBucket(bucketname);
    s3
  }

  def resize(i: BufferedImage): File = {
    import org.imgscalr.Scalr
    import javax.imageio.ImageIO

    val f = new File("tmp/" + UUID.randomUUID().toString + "_scaled.jpg")
    val si = Scalr.resize(i, 1000)
    ImageIO.write(si, "jpg", f)
    f
  }

	def loadProps() : Map[Symbol,String] = {
		import java.io.FileInputStream
		import java.io.IOException
		import java.io.InputStream
		import java.util.Properties
		
		try {
			val file = new FileInputStream("HowManyThings.properties")
			val prop = new Properties()
			
			val key = prop.getProperty("AWSKey")
			val secret = prop.getProperty("AWSSecret")
			val sandbox = prop.getProperty("SandboxMode")
			val bingkey = prop.getProperty("BingKey")
			
			if (key == null ||
				  secret == null ||
				  sandbox == null ||
				  bingkey == null) {
					println("Valid HowManyThings.properties keys:\n" +
						      "AWSKey\t= [AWS key]\n" +
						      "AWSSecret\t= [AWS secret key]\n" +
						      "BingKey\t= [Bing key]\n" +
						      "SandboxMode\t= [true/false]\n")
					System.exit(1)
				}
			
			Map('key -> prop.getProperty("AWSKey"),
				  'secret -> prop.getProperty("AWSSecret"),
				  'sandbox -> prop.getProperty("SandboxMode"),
				  'bingkey -> prop.getProperty("BingKey")) 
		} finally {
			file.Close()
		}
	}
}
