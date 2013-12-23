import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.adapters.MTurk.question.MTQuestionOption
import edu.umass.cs.automan.core.Utilities

object CanonicalizeUniversityName extends App {
  val opts = my_optparse(args, "CanonicalizeUniversityName")

  val lines = scala.io.Source.fromFile(opts('filename)).getLines().toArray

  Convert(lines).foreach(line => Console.println(line))

  private def my_optparse(args: Array[String], invoked_as_name: String) : Utilities.OptionMap = {
    val usage = "Usage: " + invoked_as_name + " -k [key] -s [secret] -f [name list] [--sandbox [true | false]]" +
      "\n  The name list be a text file, one name per line." +
      "\n  NOTE: passing key and secret this way will expose your" +
      "\n  credentials to users on this system."
    if (args.length != 6 || args.length != 8) {
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    nextOption(Map(),arglist)
  }

  private def nextOption(map : Utilities.OptionMap, list: List[String]) : Utilities.OptionMap = {
    list match {
      case Nil => map
      case "-k" :: value :: tail => nextOption(map ++ Map('key -> value), tail)
      case "-s" :: value :: tail => nextOption(map ++ Map('secret -> value), tail)
      case "-f" :: value :: tail => nextOption(map ++ Map('filename -> value), tail)
      case "--sandbox" :: value :: tail => nextOption(map ++ Map('sandbox -> value), tail)
      case option :: tail => println("Unknown option "+option)
      sys.exit(1)
    }
  }

  def Convert(stupid_names: Array[String]) : Array[String] = {
    val a = MTurkAdapter { mt =>
      mt.access_key_id = opts('key)
      mt.secret_access_key = opts('secret)
      mt.sandbox_mode = opts('sandbox).toBoolean
    }

    def _am_convert(stupid_name: String, options: List[MTQuestionOption]) = a.RadioButtonQuestion { q =>
      q.text = "Which of the following options is the same as \"$stupid_name\"?"
      q.options = options
    }

    stupid_names
  }
}
