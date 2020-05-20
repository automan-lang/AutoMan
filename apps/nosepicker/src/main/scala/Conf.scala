import org.rogach.scallop.ScallopConf

case class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  banner("""
WoCMan Nose Finder demo app

Example: java -jar NoseFinder.jar -k <MTKey> -s <MTSecret> -i images.txt -o <outfile.csv> -n <numImages> [-f <DBPath>]

For usage see below:
         """)

  val key = opt[String]("key", descr = "AWS key", required = true)
  val secret = opt[String]("secret", descr = "AWS secret key", required = true)
  val sandbox = toggle("sandbox", prefix = "no-", descrYes = "Use MTurk sandbox", descrNo = "Use real MTurk", default = Some(true))
  val database_path = opt[String]("db-path", short = 'f', descr = "Path to pre-existing memo DB.", required = false)
  val images = opt[String]("image-manifest", short = 'i', descr = "Path to text file containing image URLs, one per line.", required = true)
  val output = opt[String]("output", short = 'o', descr = "Path to output CSV.", required = true)
  val numImages = opt[Int]("numImages", short = 'n', descr = "The number of randomly-selected images to select.", required = true)
  val help = opt[Boolean]("help", noshort = true, descr = "Show this message")
}
