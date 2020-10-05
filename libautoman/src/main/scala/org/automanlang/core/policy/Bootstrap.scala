package org.automanlang.core.policy

import scala.util.Random

object Bootstrap {
  /**
    * Returns the low bound, estimate, and high bound for a function of a random variable.
    * @param X A set of random variables.
    * @param statistic A function from a set of random variables to a set of estimates.
    * @param B The number of bootstrap replications.
    * @param alpha The tolerable error.
    * @return Low bound, estimate, and high bound.
    */
  def bootstrap_CI(X: Seq[Double], statistic: Seq[Double] => Double, B: Int, alpha: Double) : (Double,Double,Double) = {
    val X2 = Seq(X)
    val statistic2 = (Y: Seq[Seq[Double]]) => Seq(statistic(Y.head))

    val (lows,theta_hats,highs) = multi_bootstrap_CIs(X2, statistic2, B, alpha)

    (lows.head,theta_hats.head,highs.head)
  }

  /**
    * Returns the low bound, estimate, and high bound for a function of a set of random variables.
    * @param X A set of random variables.
    * @param statistic A function from a set of random variables to a set of estimates.
    * @param B The number of bootstrap replications.
    * @param alpha The tolerable error.
    * @return Low bound, estimate, and high bound.
    */
  def multi_bootstrap_CI(X: Seq[Seq[Double]], statistic: Seq[Seq[Double]] => Double, B: Int, alpha: Double) : (Double,Double,Double) = {
    val statistic2 = (Y: Seq[Seq[Double]]) => Seq(statistic(Y))

    val (lows,theta_hats,highs) = multi_bootstrap_CIs(X, statistic2, B, alpha)

    (lows.head,theta_hats.head,highs.head)
  }

  /**
    * Returns a vector of low bounds, estimates, and high bounds for a function of a set of random variables.
    * @param X A set of random variables.
    * @param statistic A function from a set of random variables to a set of estimates.
    * @param B The number of bootstrap replications.
    * @param alpha The tolerable error.
    * @return A vector of low bounds, estimates, and high bounds.
    */
  def multi_bootstrap_CIs(X: Seq[Seq[Double]], statistic: Seq[Seq[Double]] => Seq[Double], B: Int, alpha: Double) : (Seq[Double],Seq[Double],Seq[Double]) = {
    // compute statistics
    val theta_hats: Seq[Double] = statistic(X)

    // compute bootstrap replications
    val replications: Seq[Seq[Double]] = (1 to B).map { b =>
        statistic(resampleWithReplacement(X))
    }

    // compute lower bound
    val lows = replications.map { x => cdfInverse(alpha / 2.0, x) }

    // compute upper bound
    val highs = replications.map { x => cdfInverse(1.0 - (alpha / 2.0), x) }

    // return
    (lows, theta_hats, highs)
  }

  /**
    * Returns a vector X' formed from resampling with
    * replacement from equal-length x \in X with uniform probability.
    * @param X vector of RVs
    * @return X' vector of RVs
    */
  private def resampleWithReplacement(X: Seq[Seq[Double]]) : Seq[Seq[Double]] = {
    X.map { x => Array.fill(x.length)(x(Random.nextInt(x.length))).toSeq }
  }

  /**
    * Given a threshold value, computes the proportion of values < t
    * @param t threshold
    * @param X input data vector
    * @return a proportion
    */
  private def cdf(t: Double, X: Seq[Double]) : Double = {
    val inds = X.indices.map(indicator(_,t,X))
    inds.sum / X.length.toDouble
  }

  /**
    * For a given p value, find the value of x in X such that p <= Pr(x)
    * @param p cumulative probability
    * @param X input data vector
    * @return a value of x in X
    */
  private def cdfInverse(p: Double, X: Seq[Double]) : Double = {
    val XSorted = X.sorted
    var i = 0
    var t: Double = XSorted(i)
    var Pr = 0.0
    while(Pr <= p) {
      Pr = cdf(t, X)
      if (i < XSorted.length - 1) {
        i += 1
        t = XSorted(i)
      } else {
        return t
      }
    }
    t
  }

  /**
    * Indicator function.
    * @param b index
    * @param t threshold
    * @param X an input vector
    * @return 1 if true, 0 if false
    */
  private def indicator(b: Int, t: Double, X: Seq[Double]) : Int = {
    if (X(b) < t) 1 else 0
  }
}
