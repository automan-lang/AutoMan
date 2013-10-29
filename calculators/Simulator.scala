import scala.util.Random

object Simulator {
  val LotsOfIterations: Int = 100000

  def RequiredForAgreement(options: Int, trials: Int, confidence: Double, iterations: Int) : Option[Int] = {
    val rng = new Random()
    val occurrences = new Array[Int](trials + 1)

    // run Monte Carlo simulation
    for (i <- 0 until iterations) {
      Iterate(options, trials, occurrences, rng)
    }

    CalculateMinTrialsForAgreement(trials, confidence, iterations, occurrences)
  }

  def Iterate(options: Int, trials: Int, occurrences: Array[Int], r: Random) : Unit = {
    val counter = new Array[Int](options + 1)

    // randomly select one of k options for j trials
    for (j <- 0 until trials) {
      counter(r.nextInt(options)) += 1
    }

    // save the size of the largest bin
    val binidx = counter.reduce((biggest, bin) => Math.max(biggest, bin))
    occurrences(binidx) += 1
  }

  def CalculateMinTrialsForAgreement(trials: Int, confidence: Double, iterations: Int, occurrences: Array[Int]) : Option[Int] = {
    var i = 1
    var p = 1.0
    val alpha = 1.0 - confidence
    while (i <= trials && p > alpha) {
      var p_i = occurrences(i).toDouble / iterations.toDouble
      p -= p_i
      i += 1
    }

    if (i <= trials && p <= alpha) {
      Some(i)
    } else {
      None
    }
  }

  // how many trials must I stack up to wash you out of my mind, out of my consciousness?
  def HowManyMoreTrials(existing_trials: Int, agreement: Int, options: Int, confidence: Double) : Int = {
    var num_addl_trials = 0
    var done = false
    while (!done) {
      val expected = agreement + num_addl_trials
      val min_required = RequiredForAgreement(options, existing_trials + num_addl_trials, confidence, LotsOfIterations)
      min_required match {
        case Some(min_req) =>
          if (min_req > expected) {
            num_addl_trials += 1
          } else {
            done = true
          }
        case None =>
          num_addl_trials += 1
      }
    }
    num_addl_trials
  }

  def invoked_as_name : String = {
//    Simulator.getClass.getProtectionDomain.getCodeSource.getLocation.getPath.split("/").last
    "Simulator"
  }

  def usage() : Unit = {
    val usage = "Usage: \n" +
      invoked_as_name + " -r [num options] [num trials] [confidence]\n" +
      " or\n" +
      invoked_as_name + " -m [num trials already run] [num that agree] [num options] [confidence]"
    println(usage)
  }

  def validate_numopts(num_opts: String) : Int = {
    val num_opts_i = num_opts.toInt
    if (num_opts_i < 2) {
      println("ERROR: num_opts must be at least 2.")
      usage()
      sys.exit(1)
    } else {
      num_opts_i
    }
  }
  
  def validate_numtrials(num_trials: String) : Int = {
    val num_trials_i = num_trials.toInt
    if (num_trials_i < 1) {
      println("ERROR: num_trials must be at least 1.")
      usage()
      sys.exit(1)
    } else {
      num_trials_i
    }
  }
  
  def validate_numagree(num_agree: String) : Int = {
    val num_agree_i = num_agree.toInt
    if (num_agree_i < 0) {
      println("ERROR: num_agree must be at least zero.")
      usage()
      sys.exit(1)
    } else {
      num_agree_i
    }
  }
  
  def validate_numrun(num_run: String) : Int = {
    val num_run_i = num_run.toInt
    if (num_run_i < 0) {
      println("ERROR: num_run must be at least zero.")
      usage()
      sys.exit(1)
    } else {
      num_run_i
    }
  }
  
  def validate_confidence(confidence: String) : Double = {
    val conf_d = confidence.toDouble
    if (conf_d < 0 || conf_d > 1) {
      println("ERROR: confidence must be between zero and one inclusive.")
      usage()
      sys.exit(1)
    } else {
      conf_d
    }
  }

  def parseOptions(arglist: List[String]) : () => Option[Int] = {
    if (arglist.length < 4 || arglist.length > 5) {
      usage()
      sys.exit(1)
    }

    arglist match {
      case "-r" :: num_opts :: num_trials :: confidence :: _ =>
        () => RequiredForAgreement(validate_numopts(num_opts),
                                   validate_numtrials(num_trials),
                                   validate_confidence(confidence),
                                   LotsOfIterations)
      case "-m" :: num_run :: num_agree :: num_opts :: confidence :: _ =>
        () => Some(HowManyMoreTrials(validate_numrun(num_run),
                                     validate_numagree(num_agree),
                                     validate_numopts(num_opts),
                                     validate_confidence(confidence)))
      case _ => {
        usage()
        sys.exit(1)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val fn = parseOptions(args.toList)

    fn() match {
      case Some(i) => println(i)
      case None => println("Insufficent number of trials to establish confidence.")
    }
  }
}
