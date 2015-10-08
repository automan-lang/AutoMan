import com.amazonaws.services.s3.AmazonS3Client
import java.io.File
import java.util.UUID
import java.awt.image.BufferedImage

package object hmtlib {
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

  def store_in_s3(si: File, s3: AmazonS3Client, bucketname: String) : String = {
    import java.util.Calendar
    import com.amazonaws.services.s3.model.{PutObjectRequest, CannedAccessControlList}

    s3.putObject(new PutObjectRequest(bucketname, si.getName, si).withCannedAcl(CannedAccessControlList.PublicRead))
    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, 2)
    s3.generatePresignedUrl(bucketname, si.getName, cal.getTime).toString
  }

  def init_s3(key: String, secret: String, bucketname: String) : AmazonS3Client = {
    import com.amazonaws.auth.BasicAWSCredentials

    val awsAccessKey = key
    val awsSecretKey = secret
    val c = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
    val s3 = new AmazonS3Client(c)
    s3.createBucket(bucketname)
    s3
  }

  def resize(i: BufferedImage): File = {
    import org.imgscalr.Scalr
    import javax.imageio.ImageIO

    val dir = new File("tmp")
    if (!dir.exists()) { dir.mkdir() }

    val f = new File(dir, UUID.randomUUID().toString + "_scaled.jpg")
    val si = Scalr.resize(i, 1000)
    ImageIO.write(si, "jpg", f)
    f
  }

  def usage(exitCondition: Int) : Unit = {
    println("Usage: HowManyThings [search term]")
    println("You must also provide a file called " +
            "\"HowManyThings.properties\"\ncontaining the " +
            "following keys:\n" +
            "AWSKey\t\t= [AWS key]\n" +
            "AWSSecret\t= [AWS secret key]\n" +
            "BingKey\t\t= [Bing key]\n" +
            "SandboxMode\t= [true/false]\n")
    System.exit(exitCondition)
  }

  def getProp(p: java.util.Properties, key: String) : String = {
    val value = p.getProperty(key)
    if (value == null) {
      println("ERROR: key \"" + key + "\" must be set.")
      usage(1)
    }
    value
  }

  def config(args: Array[String]) : Map[Symbol,String] = {
  	import java.io.FileInputStream
  	import java.util.Properties

    if (args.length != 1) {
      println("ERROR: A search term must be provided.")
      usage(1)
    }

    try {
      val file = new FileInputStream("HowManyThings.properties")
      try {
        val prop = new Properties()
        prop.load(file)

        Map('key -> getProp(prop, "AWSKey"),
          'secret -> getProp(prop, "AWSSecret"),
          'sandbox -> getProp(prop, "SandboxMode"),
          'bingkey -> getProp(prop, "BingKey"))
      } finally {
        file.close()
      }
    } catch {
      case e: java.io.FileNotFoundException => {
        usage(1)
        throw e
      }
    }
  }
}