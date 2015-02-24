package edu.umass.cs.automan.core.util

import java.io.File
import java.text.NumberFormat
import java.util.{Calendar, Date, Locale, UUID}

import edu.umass.cs.automan.core.logging.{LogType, LogLevel}

import scala.reflect.ClassTag

object Utilities {
  type OptionMap = Map[Symbol, String]

  // borrowed from: http://aperiodic.net/phil/scala/s-99/p20.scala
  def removeAt[A](n: Int, ls: List[A]): (List[A], A) = ls.splitAt(n) match {
    case (Nil, _) if n < 0 => throw new NoSuchElementException
    case (pre, e :: post)  => (pre ::: post, e)
    case (pre, Nil)        => throw new NoSuchElementException
  }

  // borrowed from: http://aperiodic.net/phil/scala/s-99/p23.scala
  def randomSelect1[A](n: Int, ls: List[A]): List[A] = {
    if (n <= 0) Nil
    else {
      val (rest, e) = removeAt((new java.util.Random).nextInt(ls.length), ls)
      e :: randomSelect1(n - 1, rest)
    }
  }
  def randomSelect[A](n: Int, ls: List[A]): List[A] = {
    def randomSelectR(n: Int, ls: List[A], r: java.util.Random): List[A] =
      if (n <= 0) Nil
      else {
        val (rest, e) = removeAt(r.nextInt(ls.length), ls)
        e :: randomSelectR(n - 1, rest, r)
      }
    randomSelectR(n, ls, new java.util.Random)
  }

  // borrowed from: http://aperiodic.net/phil/scala/s-99/p25.scala
  def randomPermute1[A](ls: List[A]): List[A] = randomSelect(ls.length, ls)
  def randomPermute[A: ClassTag](ls: List[A]): List[A] = {
    val rand = new java.util.Random
    val a = ls.toArray
    for (i <- a.length - 1 to 1 by -1) {
      val i1 = rand.nextInt(i + 1)
      val t = a(i)
      a.update(i, a(i1))
      a.update(i1, t)
    }
    a.toList
  }

  def expireTimeoutXfromNow(timeout_in_s: Int, multiplier: Double) = {
    val c = dateToCalendar(new Date())
    c.add(Calendar.SECOND, (timeout_in_s * multiplier).toInt)
    c.getTime
  }
  def dateToCalendar(d: Date) = {
    val c = Calendar.getInstance()
    c.setTime(d)
    c
  }
  def thirty_minutes_from_now = {
    import java.util.{Calendar, Date}

    var d = new Date()
    var c = Calendar.getInstance()
    c.setTime(d)
    c.add(Calendar.MINUTE, 30)
    c.getTime()
  }

  def unsafe_optparse(args: Array[String], invoked_as_name: String) : OptionMap = {
    val usage = "Usage: " + invoked_as_name + " -k [key] -s [secret] [--sandbox [true|false]]" +
                "\n  If --sandbox is not specified, the default setting is 'true'." +
                "\n  NOTE: passing key and secret this way will expose your" +
                "\n  credentials to users on this system."
    if (args.length != 4 && args.length != 6) {
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    val opts = nextOption(Map(),arglist)
    if(!opts.contains('sandbox)) {
      (opts ++ Map('sandbox -> true.toString())).asInstanceOf[OptionMap];
    } else {
      opts
    }
  }
  
  def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
    list match {
      case Nil => map
      case "-k" :: value :: tail => nextOption(map ++ Map('key -> value), tail)
      case "-s" :: value :: tail => nextOption(map ++ Map('secret -> value), tail)
      case "--sandbox" :: value :: tail => nextOption(map ++ Map('sandbox -> value), tail)
      case option :: tail => println("Unknown option "+option)
      sys.exit(1)
    }
  }

  def decimalAsDollars(bd: BigDecimal) : String = {
    val dbudget = bd.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
    val nf = NumberFormat.getCurrencyInstance(Locale.getDefault)
    nf.setMinimumFractionDigits(1)
    nf.setMaximumFractionDigits(2)
    nf.format(bd.doubleValue())
  }

  def base64Encode(file: File) : String = {
    import java.nio.file.Files
    import javax.xml.bind.DatatypeConverter

    DatatypeConverter.printBase64Binary(Files.readAllBytes(file.toPath))
  }

  def dateToTimestamp(d: java.util.Date) : Long = {
    d.getTime() / 1000
  }
}