object NumToRun {
  val LOWER = 1
  val UPPER = 2

  def main(args: Array[String]) {
    if (args.length < 3 || args.length > 4) {
      println("Computes how many trials are needed to determine that a coin/die is biased with the given bias, # faces, and confidence.")
      println("Usage: sbt \"run <bias probability> <# sides> <confidence> [--upper|--lower]\"")
      println("Example:\n\nsbt \"run 0.75 2 0.95 --upper\"\n")
      System.exit(1)
    }

    val p_succ = args(0).toDouble
    val opts = args(1).toInt
    val conf = args(2).toDouble
    val testType =
      (if (args.length == 4) {
        args(3) match {
          case "--upper" => UPPER
          case "--lower" => LOWER
        }
      } else {
        println("Unknown tail.  Provide either --upper or --lower.")
        System.exit(1)
      })

    // if the "coin" is unbiased, this would be the probability
    val unbiased_p = 1.0/opts
    var trials = 0
    var empirical_conf = 0.00

    do {
      // add a trial
      trials += 1

      // compute the expected number of observed successes
      // given the actual (biased) probability of success
      val successes = (trials * p_succ).toInt

      // compute Chernoff bound with the hypothesis that the
      // "coin" is unbiased
      val upper = upperBound(trials, successes, unbiased_p)
      val lower = upperBound(trials, successes, unbiased_p)

      // update empirical confidence
      testType match {
        case LOWER => empirical_conf = 1.0 - lower
        case UPPER => empirical_conf = 1.0 - upper
      }

    } while (empirical_conf < conf)
    
    println(s"Need ${trials} trials to demonstrate bias of ${p_succ} with confidence = ${empirical_conf}.")
  }

  /**
   * Compute epsilon for use in the upper Chernoff bound.
   * @param n Number of trials.
   * @param t Number of successes s.t., successes &lt;= trials.
   * @param p Hypothetical probability of success.
   * @return Epsilon
   */
  private def upperEpsilon(n: Int, t: Int, p: Double) : Double = {
    val μ = n * p
    t/μ - 1
  }

  /**
   * Compute epsilon for use in the lower Chernoff bound.
   *
   * @param n Number of trials.
   * @param t Number of successes s.t., successes &lt;= trials.
   * @param p Hypothetical probability of success.
   * @return Epsilon
   */
  private def lowerEpsilon(n: Int, t: Int, p: Double): Double = {
    val μ = n * p
    1 - t / μ
  }

  /**
   * Compute the upper bound.
   * @param trials Number of trials.
   * @param n_succ Number of successes s.t., successes &lt;= trials.
   * @param p Hypothetical probability of success.
   * @return
   */
  private def upperBound(trials: Int, n_succ: Int, p: Double) : Double = {
    assert(n_succ <= trials)
    val ϵ = upperEpsilon(trials, n_succ, p)
    val μ = trials * p
    Math.exp(-((ϵ*ϵ)/(2+ϵ))*μ)
  }

  /**
   * Compute the lower bound.
   *
   * @param trials Number of trials.
   * @param n_succ Number of successes s.t., successes &lt;= trials.
   * @param p Hypothetical probability of success.
   * @return
   */
  private def lowerBound(trials: Int, n_succ: Int, p: Double): Double = {
    assert(n_succ <= trials)
    val ϵ = lowerEpsilon(trials, n_succ, p)
    val μ = trials * p
    Math.exp(-((ϵ * ϵ) / 2) * μ)
  }
}