import scala.util.Random

object Simulator {
  val NumberOfSimulations: Int = 100000

  def GetMinTrialsForAgreement(options: Int, confidence: Double, simulations: Int) : Int = {
    var done = false
    var trials = 1
    while(!done) {
      RequiredForAgreement(options, trials, confidence, simulations) match {
        case Some(t) => done = true
        case None => trials += 1
      }
    }
    trials
  }

  def RequiredForAgreement(choices: Int, trials: Int, confidence: Double, simulations: Int) : Option[Int] = {
    val outcomes = SimulateNTimes(choices, trials, simulations)

    CalculateMinTrialsForAgreement(trials, confidence, simulations, outcomes)
  }

  def SimulateNTimes(choices: Int, trials: Int, simulations: Int) : Array[Int] = {
    val rng = new Random()
    val outcomes = new Array[Int](trials + 1)

    // run Monte Carlo simulation
    for (i <- 0 until simulations) {
      // simulate flipping a (1/choices) coin for (trials) trials and return the size of the winning bin
      // which must always be <= the number of trials
      val max_agreement = Simulate(choices, trials, rng)
      // update max_agreement count
      outcomes(max_agreement) += 1
    }
    
    outcomes
  }

  def Simulate(choices: Int, trials: Int, r: Random) : Int = {
    val histogram = new Array[Int](choices)

    // randomly select one of k choices for j trials
    for (j <- 0 until trials) {
      histogram(r.nextInt(choices)) += 1
    }

    // return the size of the biggest bin
    histogram.reduce((biggest, bin) => Math.max(biggest, bin))
  }

  def CalculateProbability(choices: Int, trials: Int, num_agree: Int, simulations: Int) : Double = {
    val outcomes = SimulateNTimes(choices, trials, simulations)
    
    // return the probability that (num_agree) agreements occur
    outcomes(num_agree).toDouble / simulations.toDouble
  }

  def CalculateMinTrialsForAgreement(trials: Int, confidence: Double, simulations: Int, outcomes: Array[Int]) : Option[Int] = {
    var i = 1
    var p = 1.0
    val alpha = 1.0 - confidence
    while (i <= trials && p > alpha) {
      var p_i = outcomes(i).toDouble / simulations.toDouble
      p -= p_i
      i += 1
    }
  
    if (i <= trials && p <= alpha) {
      Some(i)
    } else {
      None
    }
  }

  def HowManyMoreTrials(existing_trials: Int, agreement: Int, options: Int, confidence: Double) : Int = {
    var num_addl_trials = 0
    var done = false
    while (!done) {
      val expected = agreement + num_addl_trials
      val min_required = RequiredForAgreement(options, existing_trials + num_addl_trials, confidence, NumberOfSimulations)
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
    "Simulator"
  }

  def usage() : Unit = {
    val usage = "Usage: \n" +
      "To determine the minimum number of trials to run for a given number of choices and confidence:\n" +
      "\t" + invoked_as_name + " -r [num choices] [confidence]\n" +
      "or\n" +
      "To determine how many more trials to run for a given number of agreements, choices, and confidence:\n" +
      "\t" + invoked_as_name + " -m [num trials already run] [num that agree] [num choices] [confidence]\n" +
      "or\n" +
      "To determine the (approximate) probability of having a given number of agreements for some number of choices and trials:\n" +
      "\t" + invoked_as_name + " -p [num choices] [num trials] [num that agree]"
    println(usage)
  }

  def validate_numchoices(num_choices: String) : Int = {
    val num_choices_i = num_choices.toInt
    if (num_choices_i < 2) {
      println("ERROR: num_choices must be at least 2.")
      usage()
      sys.exit(1)
    } else {
      num_choices_i
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

  def parseOptions(arglist: List[String]) = {
    arglist match {
      case "-r" :: num_choices :: confidence :: _ =>
        () => GetMinTrialsForAgreement(validate_numchoices(num_choices),
                                       validate_confidence(confidence),
                                       NumberOfSimulations)
      case "-m" :: num_run :: num_agree :: num_choices :: confidence :: _ =>
        () => HowManyMoreTrials(validate_numrun(num_run),
                                validate_numagree(num_agree),
                                validate_numchoices(num_choices),
                                validate_confidence(confidence))
      case "-p" :: num_choices :: num_trials :: num_agree :: _ =>
        () => CalculateProbability(validate_numchoices(num_choices),
                                        validate_numtrials(num_trials),
                                        validate_numagree(num_agree),
                                        NumberOfSimulations)
      case _ => {
        usage()
        sys.exit(1)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val fn = parseOptions(args.toList)

    println(fn())
  }
}
