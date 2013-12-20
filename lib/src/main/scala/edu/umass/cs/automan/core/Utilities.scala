package edu.umass.cs.automan.core

import java.util.{UUID, Date, Calendar}
import java.util


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
      val (rest, e) = removeAt((new util.Random).nextInt(ls.length), ls)
      e :: randomSelect1(n - 1, rest)
    }
  }
  def randomSelect[A](n: Int, ls: List[A]): List[A] = {
    def randomSelectR(n: Int, ls: List[A], r: util.Random): List[A] =
      if (n <= 0) Nil
      else {
        val (rest, e) = removeAt(r.nextInt(ls.length), ls)
        e :: randomSelectR(n - 1, rest, r)
      }
    randomSelectR(n, ls, new util.Random)
  }

  // borrowed from: http://aperiodic.net/phil/scala/s-99/p25.scala
  def randomPermute1[A](ls: List[A]): List[A] = randomSelect(ls.length, ls)
  def randomPermute[A: ClassManifest](ls: List[A]): List[A] = {
    val rand = new util.Random
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
    import java.util.Date
    import java.util.Calendar

    var d = new Date()
    var c = Calendar.getInstance()
    c.setTime(d)
    c.add(Calendar.MINUTE, 30)
    c.getTime()
  }

  def DebugLog(msg: String, level: LogLevel.Value, source: LogType.Value, id: UUID) {
    val idstr = source match {
      case LogType.SCHEDULER => "question_id = " + id.toString + ", "
      case LogType.STRATEGY => "computation_id = " + id.toString + ", "
      case LogType.ADAPTER => "question_id = " + id.toString + ", "
      case LogType.MEMOIZER => ""
    }

    println(new Date().toString + ": " + level.toString + ": " + source.toString + ": " + idstr +  msg)
  }

  def unsafe_optparse(args: Array[String], invoked_as_name: String) : OptionMap = {
    val usage = "Usage: " + invoked_as_name + " -k [key] -s [secret] [--sandbox [true|false]]" +
                "\n  If --sandbox is not specified, the default setting is 'true'." +
                "\n  NOTE: passing key and secret this way will expose your" +
                "\n  credentials to users on this system."
    if (args.length != 4) {
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    val opts = nextOption(Map(),arglist)
    if(!opts.contains('sandbox)) {
      (opts ++ Map('sandbox -> true)).asInstanceOf[OptionMap];
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
}

object LogType extends Enumeration {
  type LogType = Value
  val STRATEGY,
  SCHEDULER,
  ADAPTER,
  MEMOIZER
  = Value
}

object LogLevel extends Enumeration {
  type LogLevel = Value
  val INFO,
  WARN,
  FATAL
  = Value
}

import LogType._
import LogLevel._