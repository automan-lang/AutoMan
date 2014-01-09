package main.scala

import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.core.Utilities
import scala.util.Random
import edu.umass.cs.automan.adapters.MTurk.question.MTFreeTextQuestion

object Spamazon extends App {
  val Items = 20
  val MinLen = 3
  val MaxLen = 32
  val ASCII_A = 65
  val ASCII_Z = 90
  val ASCII_a = 97
  val ASCII_z = 122
  val opts = Utilities.unsafe_optparse(args, "Spamazon")

  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
  }

  val rng = new Random()
  val lengths = (0 until Items).map { _ => rng.nextInt(MaxLen - MinLen) + MinLen}
  val strs = lengths.map { len => (0 until len).foldLeft("")((acc, i) => acc + randomChar(rng).toString())}

  println(strs.length + " random strings generated.")

  val answers = (0 until Items).map { i => CountCaps(strs(i), i)}

  // force parallel evaluation
  val outstrings = (0 until Items).par.map { i => i + ": String: " + strs(i) + "\n" + i + ": Number of caps: " + answers(i)() }

  for(str <- outstrings) {
    println(str)
  }

//  for(i <- 0 until strs.length) {
//    println(i + ": " + strs(i))
//  }
  println("Done.")

  def CountCaps(str: String, i: Int) = a.FreeTextQuestion { q =>
    System.err.println("Posting hit for string #" + i)
    q.title = "Count the number of capital letters in this text."
    q.text = "How many capital letters are in the following text:\n\n\"" + str + "\""
    q.pattern = "YX"
  }

  def randomChar(rng: Random) : Char = {
    val maxint = ASCII_Z - ASCII_A + ASCII_z - ASCII_a
    val cap_threshold = ASCII_Z - ASCII_A
    val rint = rng.nextInt(maxint)
    if (rint >= cap_threshold) {
      ((rint - cap_threshold) + ASCII_a).toChar
    } else {
      (rint + ASCII_A).toChar
    }
  }
}
