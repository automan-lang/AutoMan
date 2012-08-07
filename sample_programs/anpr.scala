import collection.mutable
import com.amazonaws.services.s3.AmazonS3Client
import edu.umass.cs.automan.core.Utilities
import java.io.File
import edu.umass.cs.automan.adapters.MTurk._

object anpr extends App {
  val opts = my_optparse(args)
  val bucketname = opts('directory).split("/").last.replaceAll("/","").replaceAll("_","")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = false
  }

  def get_plate_text(image_url: String) = a.FreeTextQuestion { q =>
    q.budget = 5.00
    q.text = "What are the characters printed on this license plate? NO DASHES, DOTS OR SPACES! If the plate is unreadable, enter: NA"
    q.image_url = image_url
    q.allow_empty_pattern = true
    q.pattern = "XXXXXYYY"
    q.pattern_error_text = "Answers may only be letters or numeric digits, no more than 8 characters long, no spaces, or the special two-letter code: NA"
    q.dont_reject = true
    q.before_filter = normalize_chars
  }

  // get a list of image names on the local filesystem
  val filehandles = (new java.io.File(opts('directory))).listFiles.toList
  val url_fn_map = scala.collection.mutable.Map[String,String]()

  // store each image in S3
  print("Uploading files.  This may take awhile.")
  val s3client: AmazonS3Client = init_s3()
  val s3_urls: List[String] = filehandles.map{ i =>
    val url = store_in_s3(i, s3client)
    print(".")
    url_fn_map += (url -> i.toString().split("/").last)
    url
  }
  println()

  // get plate texts for the good ones
  val url_answer_map = scala.collection.mutable.Map[String,Symbol]()
  val fn_cost_map = scala.collection.mutable.Map[String,BigDecimal]()
  val plate_texts = s3_urls.par.map { url =>
    val fd = get_plate_text(url)()
    val answer = fd.value
    url_answer_map += (url -> answer)
    answer
  }

  // print out results
  plate_texts.foreach { text => println(text) }

  // print out score
  var correct_count = 0
  var total_count = 0
  url_answer_map.foreach{ case(url,answer) =>
    val filename = url_fn_map(url)
    if (answer_key(filename, answer)) {
      correct_count += 1
      println("CORRECT: " + filename + " = " + answer.toString() + " for url: " + url)
    } else {
      println("INCORRECT: " + filename + " = " + answer.toString() + " for url: " + url)
    }
    total_count += 1
  }
  println("Total score: " + (correct_count.toFloat/total_count.toFloat * 100.0).toString + "% correct")

  // helper functions
  def store_in_s3(si: File, s3: AmazonS3Client) : String = {
    import java.util.Calendar
    import com.amazonaws.services.s3.model.{PutObjectRequest, CannedAccessControlList}

    s3.putObject(new PutObjectRequest(bucketname, si.getName, si).withCannedAcl(CannedAccessControlList.PublicRead))
    val cal = Calendar.getInstance()
    cal.add(Calendar.WEEK_OF_YEAR, 2)
    s3.generatePresignedUrl(bucketname, si.getName, cal.getTime).toString
  }

  def init_s3() : AmazonS3Client = {
    import com.amazonaws.auth.BasicAWSCredentials

    val awsAccessKey = opts('key)
    val awsSecretKey = opts('secret)
    val c = new BasicAWSCredentials(awsAccessKey, awsSecretKey)
    val s3 = new AmazonS3Client(c)
    s3.createBucket(bucketname)
    s3
  }

  def my_optparse(args: Array[String]) : Utilities.OptionMap = {
    val usage = "Usage: " + Utilities.invoked_as_name + " -k [key] -s [secret] -d [image directory]" +
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
      case "-d" :: value :: tail => nextOption(map ++ Map('directory -> value), tail)
      case option :: tail => println("Unknown option "+option)
      sys.exit(1)
    }
  }

  def answer_key(filename: String, answer: Symbol) : Boolean = {
    val answers = Map(
      "I1000267.jpg" -> 'C00252TA,
      "I1000268.jpg" -> 'PNP4292,
      "I1000270.jpg" -> 'C7053PK,
      "I1000271.jpg" -> 'CA10127,
      "I1000272.jpg" -> 'E0831AK,
      "I1000273.jpg" -> 'C08430TA,
      "I1000274.jpg" -> 'BT079CD,
      "I1000275.jpg" -> 'PNP4327,
      "I1000276.jpg" -> 'PNN3988,
      "I1000277.jpg" -> Symbol("57642"),
      "I1000278.jpg" -> 'C0314KX,
      "I1000279.jpg" -> 'SK305JN,
      "I1000280.jpg" -> 'PNN3712,
      "I1000281.jpg" -> 'PNP4048,
      "I1000282.jpg" -> 'PN08275,
      "I1000283.jpg" -> 'C0314KX,
      "I1000284.jpg" -> 'BG360272,
      "I1000285.jpg" -> 'PNN3339,
      "I1000286.jpg" -> 'PNN3339,
      "I1000290.jpg" -> 'PNP7018,
      "I1000291.jpg" -> 'PNP4043,
      "I1000292.jpg" -> 'PNN6328,
      "I1000293.jpg" -> 'PNN6328,
      "I1000294.jpg" -> 'PNN6328,
      "I1000295.jpg" -> 'PNN2734,
      "I1000296.jpg" -> 'ST1160N,
      "I1000297.jpg" -> 'ST1360N,
      "I1000298.jpg" -> 'ST0140M,
      "I1000300.jpg" -> 'E8093AX,
      "I2000001.jpg" -> 'PNM8674,
      "I2000002.jpg" -> 'PNI5696,
      "I2000004.jpg" -> 'PNP4412,
      "I2000008.jpg" -> 'PNN6328,
      "I2000009.jpg" -> 'PN09682,
      "I2000012.jpg" -> 'PNP3813,
      "I2000014.jpg" -> 'PNN3906,
      "I2000015.jpg" -> 'PNP3445,
      "I2000016.jpg" -> 'PN09405,
      "I2000017.jpg" -> 'PNN2853,
      "I2000021.jpg" -> 'PNP4327,
      "I2000057.jpg" -> 'PNN3715,
      "I2000063.jpg" -> 'PNP4416,
      "I2000064.jpg" -> 'PNP4416,
      "I2000072.jpg" -> 'PNP4415,
      "I2000080.jpg" -> Symbol("57642"),
      "I2000082.jpg" -> Symbol("57642"),
      "I2000083.jpg" -> Symbol("57642"),
      "I2000088.jpg" -> 'NA,
      "I2000091.jpg" -> 'PNP4141,
      "I2000092.jpg" -> Symbol("57642"),
      "I2000093.jpg" -> 'PNP4387,
      "I2000094.jpg" -> Symbol("57642"),
      "I2000096.jpg" -> 'PNN3885,
      "O3000000.jpg" -> 'NA,
      "O3000003.jpg" -> 'NA,
      "O3000004.jpg" -> 'PN09682,
      "O3000005.jpg" -> 'PNP4411,
      "O3000006.jpg" -> 'PNP4411,
      "O3000007.jpg" -> 'KH08584,
      "O3000051.jpg" -> 'PNN3799,
      "O3000056.jpg" -> 'PNN3885,
      "O3000057.jpg" -> 'PNN3885,
      "O3000059.jpg" -> 'PNN3715,
      "O3000060.jpg" -> 'PNN3203,
      "O3000067.jpg" -> 'PNN3710,
      "O3000072.jpg" -> Symbol("57642"),
      "O3000074.jpg" -> 'PNP4415,
      "O3000078.jpg" -> 'PNN3799,
      "O3000080.jpg" -> 'PNP4141,
      "O3000082.jpg" -> 'PNP4236,
      "O3000086.jpg" -> 'PNN3781,
      "O3000089.jpg" -> 'PNM3381
    )

    answers(filename) == answer
  }

  def normalize_chars(s: Symbol) : Symbol = {
    val text = s.toString().drop(1)
    Symbol(text.replaceAll("o","0").replaceAll("O","0").toUpperCase)
  }
}