package main.scala

import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.core.Utilities
import edu.umass.cs.automan.adapters.MTurk.question.MTQuestionOption
import com.ebay.sdk._
import com.ebay.sdk.call.GeteBayOfficialTimeCall

object eBayCategorizer extends App {
  val opts = my_optparse(args, "eBayCategorizer")
  val ebay_soap = "https://api.ebay.com/wsapi"

  // init AutoMan for MTurk
  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  // init ebay
  val ebay = getApiContext()

  // init ebay API call
  val api = new ApiCall(ebay)

  // get time
  val apiCall = new GeteBayOfficialTimeCall(ebay)
  val cal = apiCall.geteBayOfficialTime()
  println("Official eBay Time : " + cal.getTime().toString())

  def Classify(image_path: String, options: List[MTQuestionOption]) = a.RadioButtonQuestion { q =>
    q.title = "Please choose the appropriate category for this image"
    q.text = "Please choose the appropriate category for this image"
    q.options = a.Option('none, "None of these categories apply.") :: options
  }

  private def my_optparse(args: Array[String], invoked_as_name: String) : Utilities.OptionMap = {
    val usage = "Usage: " + invoked_as_name + " -k [AWS key] -s [AWS secret] -b [Bing key] -f [output filename] [--sandbox [true | false]]" +
      "\n  NOTE: passing credentials this way will expose" +
      "\n  them to users on this system."
    if (args.length != 6 && args.length != 8) {
      println("You only supplied " + args.length + " arguments.")
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    val opts = nextOption(Map(),arglist)
    if(!opts.contains('sandbox)) {
      (opts ++ Map('sandbox -> true.toString()))
    } else {
      opts
    }
  }

  private def nextOption(map : Utilities.OptionMap, list: List[String]) : Utilities.OptionMap = {
    list match {
      case Nil => map
      case "-k" :: value :: tail => nextOption(map ++ Map('key -> value), tail)
      case "-s" :: value :: tail => nextOption(map ++ Map('secret -> value), tail)
      case "-e" :: value :: tail => nextOption(map ++ Map('ebay_key -> value), tail)
      case "--sandbox" :: value :: tail => nextOption(map ++ Map('sandbox -> value), tail)
      case option :: tail => println("Unknown option "+option)
        sys.exit(1)
    }
  }

  private def getApiContext() : ApiContext = {
    val apiContext = new ApiContext()
    apiContext.getApiCredential().seteBayToken(opts('ebay_key))
    apiContext.setApiServerUrl(ebay_soap)
    apiContext
  }
}