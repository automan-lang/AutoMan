package edu.umass.cs.automan.adapters.mturk

import org.rogach.scallop.ScallopConf

case class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  banner("""
Accept and pay for all assignments marked as unpaid in a given memo DB.

Example: AcceptAllFromDB -k <MTKey> -s <MTSecret> -d <memo database>

For usage see below:
         """)

  val key = opt[String]("key", descr = "AWS key", required = true)
  val secret = opt[String]("secret", descr = "AWS secret key", required = true)
  val sandbox = toggle("sandbox", prefix = "no-", descrYes = "Use MTurk sandbox", descrNo = "Use real MTurk", default = Some(true))
  val database_path = opt[String]("database", short = 'd', descr = "Path to H2 memo database", required = true)
  val help = opt[Boolean]("help", noshort = true, descr = "Show this message")
}
