object NumToRun {
  val LOWER = 1
  val UPPER = 2

  def main(args: Array[String]) {
    if (args.length < 3 || args.length > 4) {
      println("Computes how many trials are needed to determine that a coin/die is biased with the given bias, # faces, and confidence.")
      println("Usage: sbt \"run <bias probability> <# sides> <confidence> [--greater|--smaller]\"")
      println("Example:\n\nsbt \"run 0.75 2 0.95 --greater\"\n")
      System.exit(1)
    }

    val p_bias = args(0).toDouble
    val opts = args(1).toInt
    val conf = args(2).toDouble
    val testType =
      (if (args.length == 4) {
        args(3) match {
          case "--greater" => UPPER
          case "--smaller" => LOWER
        }
      } else {
        println("Unknown tail.  Provide either --greater or --smaller.")
        System.exit(1)
      })

    // if the "coin" is unbiased, this would be the probability
    val unbiased_p = 1.0/opts
    var trials = 1
    var empirical_conf = 0.00

    do {
      // add a trial
      trials += 1

      // compute the expected number of observed successes
      // given the actual (biased) probability of success
      val successes = (trials * p_bias).toInt

      // compute Chernoff bound with the hypothesis that the
      // "coin" is unbiased
      val upper = upperBound(trials, successes, unbiased_p)
      val lower = upperBound(trials, successes, unbiased_p)

      // update empirical confidence
      testType match {
        case LOWER => empirical_conf = 1.0 - lower
        case UPPER => empirical_conf = 1.0 - upper
      }

      println(s"The probability of ${successes} or more heads out of ${trials} flips is ${upper}")
      println(s"The probability of ${successes} or fewer heads out of ${trials} flips is ${lower}")

    } while (empirical_conf < conf)

    println(s"Need ${trials} trials to demonstrate bias of ${p_bias} with confidence = ${empirical_conf}.")
  }

  /**
   * Compute epsilon for use in the upper Chernoff bound.
   * @param n Number of trials.
   * @param t Number of successes s.t., successes &lt;= trials.
   * @param p Hypothetical probability of success.
   * @return Epsilon
   */
  private def upperEpsilon(n: Int, t: Int, p: Double) : Double = {
    val μ = n.toDouble * p
    t.toDouble/μ - 1.0
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
    val μ = n.toDouble * p
    1.0 - t.toDouble / μ
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
