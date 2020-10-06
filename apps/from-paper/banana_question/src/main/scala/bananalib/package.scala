import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, AmazonS3ClientBuilder}
import java.io.File
import java.util.UUID
import java.awt.image.BufferedImage

import com.amazonaws.client.builder.{AwsClientBuilder, AwsSyncClientBuilder}
import org.automanlang.core.util.Utilities

package object bananalib {
  def getTinyURL(long_url: String) : String = {
    import java.net.{URLConnection, URL}
    import java.io.{BufferedReader,InputStream,InputStreamReader}
    import java.net.URLEncoder

    // URLencode
    val url_enc = URLEncoder.encode(long_url, "UTF-8")

    val lurl = new URL("http://tinyurl.com/api-create.php?url=" + url_enc)

    // init accumulator
    val buf = new StringBuilder()

    // issue query and get handle to results
    val conn: URLConnection = lurl.openConnection()
    val reader: BufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()))

    // read data into buffer
    var inputLine: String = reader.readLine()
    while (inputLine != null) {
      buf.append(inputLine)
      inputLine = reader.readLine()
    }

    // close handle
    reader.close()

    // return URL
    buf.toString()
  }

  def store_in_s3(si: File, s3: AmazonS3, bucketname: String) : String = {
    import java.util.Calendar
    import com.amazonaws.services.s3.model.{PutObjectRequest, CannedAccessControlList}

    s3.putObject(new PutObjectRequest(bucketname, si.getName, si).withCannedAcl(CannedAccessControlList.PublicRead))
    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, 1)
    s3.generatePresignedUrl(bucketname, si.getName, cal.getTime).toString
  }

  def init_s3(key: String, secret: String, bucketname: String) : AmazonS3 = { // changed from AmazonS3Client
    import com.amazonaws.auth.BasicAWSCredentials

    val awsAccessKey = key
    val awsSecretKey = secret
    val c = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
    //val s3 = new AmazonS3Client(c)
    val s3 = AmazonS3ClientBuilder.defaultClient() //.standard()
    // new AmazonS3ClientBuilder.defaultClient() //TODO: or async?
    s3.createBucket(bucketname)
    s3
  }

  def resize(i: BufferedImage, width: Int): File = {
    import org.imgscalr.Scalr
    import javax.imageio.ImageIO

    val dir = new File("tmp")
    if (!dir.exists()) { dir.mkdir() }

    val f = new File(dir, UUID.randomUUID().toString + "_scaled.jpg")
    val si = Scalr.resize(i, width)
    ImageIO.write(si, "jpg", f)
    f
  }

  def my_optparse(args: Array[String], invoked_as_name: String) : Utilities.OptionMap = {
    val usage = "Usage: " + invoked_as_name + " -k [key] -s [secret] -b [sandbox]" +
      "\n  NOTE: passing key and secret this way will expose your" +
      "\n  credentials to users on this system."
    if (args.length != 6) {
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    nextOption(Map(),arglist)
  }

  def nextOption(map : Utilities.OptionMap, list: List[String]) : Utilities.OptionMap = {
    list match {
      case Nil => map
      case "-k" :: value :: tail => nextOption(map ++ Map('key -> value), tail)
      case "-s" :: value :: tail => nextOption(map ++ Map('secret -> value), tail)
      case "-b" :: value :: tail => nextOption(map ++ Map('sandbox -> value), tail)
      case option :: tail => println("Unknown option "+option)
        sys.exit(1)
    }
  }
}