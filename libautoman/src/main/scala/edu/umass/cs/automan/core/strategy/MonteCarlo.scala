package edu.umass.cs.automan.core.strategy

import scala.util.Random

// TODO: MonteCarlo simulator should take BigInts
object MonteCarlo {
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

  /**
   * Calculate the probabilty that a n-choices-sided die rolled
   * t-trials times has num_agree agreements.
   * @param choices Number of choices (sides of a die).
   * @param trials Number of trials (rolls of a die).
   * @param num_agree The number of agreements we're looking for.
   * @param simulations Number of repetitions of the experiment to run.
   * @return The probability of the event happening.
   */
  def CalculateProbability(choices: Int, trials: Int, num_agree: Int, simulations: Int) : Double = {
    val outcomes = SimulateNTimes(choices, trials, simulations)

    // return the probability that (num_agree) agreements occur
    val p = outcomes(num_agree).toDouble / simulations.toDouble

    p
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

//  def parseOptions(arglist: List[String]) = {
//    arglist match {
//      case "-r" :: num_choices :: confidence :: _ =>
//        () => GetMinTrialsForAgreement(validate_numchoices(num_choices),
//          validate_confidence(confidence),
//          NumberOfSimulations)
//      case "-m" :: num_run :: num_agree :: num_choices :: confidence :: _ =>
//        () => HowManyMoreTrials(validate_numrun(num_run),
//          validate_numagree(num_agree),
//          validate_numchoices(num_choices),
//          validate_confidence(confidence))
//      case "-p" :: num_choices :: num_trials :: num_agree :: _ =>
//        () => CalculateProbability(validate_numchoices(num_choices),
//          validate_numtrials(num_trials),
//          validate_numagree(num_agree),
//          NumberOfSimulations)
//      case _ => {
//        usage()
//        sys.exit(1)
//      }
//    }
//  }
//

}
