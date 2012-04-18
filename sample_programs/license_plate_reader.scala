import com.amazonaws.services.s3.AmazonS3Client
import edu.umass.cs.automan.core.Utilities
import java.awt.image.BufferedImage
import java.io.File
import edu.umass.cs.automan.adapters.MTurk._

object license_plate_reader extends App {
  val opts = Utilities.unsafe_optparse(args)

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = true
  }

  def is_a_car(image_url: String) = a.RadioButtonQuestion { q =>
    q.image_url = image_url
    q.text = "Is this a vehicle with a readable license plate?"
    q.options = List(
      a.Option('yes, "Yes"),
      a.Option('no, "No")
    )
  }

  def get_plate_text(image_url: String) = a.FreeTextQuestion { q =>
    q.text = "What are the characters printed on this license plate? (NO DASHES, DOTS OR SPACES please!)"
    q.image_url = image_url
    q.pattern = "XXXXXXY"
    q.pattern_error_text = "Answers may only be letters or numeric digits, no more than 7 characters long, no spaces."
  }

  // Search for a bunch of images
  val urls = get_urls("site:http://www.oldparkedcars.com 1")

  // download each image
  val images = urls.map(download_image(_))

  // resize each image
  val sc_images = images.map(resize(_))

  // store each image in S3
  val s3client = init_s3()
  val s3_urls = sc_images.map{ i => store_in_s3(i, s3client) }

  // Are these pictures of cars with license plates?
  val possible_cars = s3_urls.map( is_a_car(_) )

  // filter out the bad cars and get plate texts for the good ones
  val plate_texts = possible_cars.filter{ ans => ans().value == 'yes}.map { ans =>
    get_plate_text(ans().value.toString)
  }

  // print out results
  plate_texts.foreach { fd => println(fd().value) }

  // helper functions
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

    val c = new Client
    val results: List[Result] = (0 until 1).map { i =>
      c.searchCustomImages(query, i * 8).asScala.toList
    }.toList.flatten
    results.map { _.getUnescapedUrl }.distinct
  }

  def store_in_s3(si: File, s3: AmazonS3Client) : String = {
    import java.util.Calendar
    import com.amazonaws.services.s3.model.{PutObjectRequest, CannedAccessControlList}

    s3.putObject(new PutObjectRequest("cardata", si.getName, si).withCannedAcl(CannedAccessControlList.PublicRead))
    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, 2)
    s3.generatePresignedUrl("cardata", si.getName, cal.getTime).toString
  }

  def init_s3() : AmazonS3Client = {
    import com.amazonaws.auth.BasicAWSCredentials

    val awsAccessKey = "XXXX";
    val awsSecretKey = "XXXX";
    val c = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
    val s3 = new AmazonS3Client(c)
    s3.createBucket("cardata");
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