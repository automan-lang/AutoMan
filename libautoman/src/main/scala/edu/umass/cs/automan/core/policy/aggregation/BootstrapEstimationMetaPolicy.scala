package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.question.EstimationMetaQuestion
import edu.umass.cs.automan.core.question.confidence.{AsymmetricCI, UnconstrainedCI, SymmetricCI}
import scala.util.Random

class BootstrapEstimationMetaPolicy(q: EstimationMetaQuestion, op: Double => Double => Double) extends MetaAggregationPolicy {
  override type A = Double
  override type AA = AbstractEstimate

  val NumBootstraps = 512 * 2

  // cache to ensure that output matches test since the
  // bootstrap is a randomized algorithm
  var bootCache = Map[(Seq[Double],Seq[Double],Int,Double),(Double,Double,Double)]()

  def metaAnswer(round: Int, backend: AutomanAdapter): AA = {
    // get adjusted confidence
    val adj_conf = adjustedConfidence(round)

    // get per-question confidence
    val perQConf = Math.sqrt(adj_conf)

    // create and run new questions with higher confidence levels
    val lhs2 = q.lhs.cloneWithConfidence(perQConf)
    val rhs2 = q.rhs.cloneWithConfidence(perQConf)

    // get outcomes
    val lhs2o = lhs2.getOutcome(backend)
    val rhs2o = rhs2.getOutcome(backend)

    // compute estimate
    (lhs2o.answer,rhs2o.answer) match {
      case (e1:Estimate,e2:Estimate) =>
        // get distributions
        val X = e1.distribution.map(_.value)
        val Y = e2.distribution.map(_.value)

        // alpha
        val alpha = 1 - adj_conf

        // bootstrap
        val (low, theta_hat, high) = bootstrap_pairs(lhs2.estimator, rhs2.estimator, X, Y, NumBootstraps, alpha)

        // cost
        val cost = e1.cost + e2.cost

        Estimate(theta_hat, low, high, cost, adj_conf, null, Array.concat(e1.distribution, e2.distribution))
      case (e1:OverBudgetEstimate,_) => e1
      case (_,e2:OverBudgetEstimate) => e2
      case (e1:LowConfidenceEstimate,_) => e1
      case (_,e2:LowConfidenceEstimate) => e2
    }
  }

  /**
    * Computes statistic and confidence intervals specified.
    * @param Xstatistic An arbitrary function of the data.
    * @param Ystatistic An arbitrary function of the data.
    * @param X an input vector
    * @param Y an input vector
    * @param B the number of bootstrap replications to perform
    * @param alpha the margin of error
    * @return
    */
  def bootstrap_pairs(Xstatistic: Seq[Double] => Double, Ystatistic: Seq[Double] => Double, X: Seq[Double], Y: Seq[Double], B: Int, alpha: Double) : (Double,Double,Double) = {
    // check cache
    if (bootCache.contains((X, Y, B, alpha))) {
      bootCache((X, Y, B, alpha))
    } else {
      // compute statistics
      val theta_hat = op(Xstatistic(X))(Ystatistic(Y))

      // compute bootstrap replications
      val replications = (1 to B).map { b =>
        op(
          Xstatistic(resampleWithReplacement(X))
        )(
          Ystatistic(resampleWithReplacement(Y))
        )
      }

      // compute lower bound
      val low = cdfInverse(alpha / 2.0, replications)

      // compute upper bound
      val high = cdfInverse(1.0 - (alpha / 2.0), replications)

      // cache result
      bootCache += ((X, Y, B, alpha) -> (low, theta_hat, high))

      // return
      (low, theta_hat, high)
    }
  }

  /**
    * Returns an equal-length vector X' formed from resampling with
    * replacement from X with uniform probability.
    * @param X input data vector
    * @return X'
    */
  private def resampleWithReplacement(X: Seq[Double]) : Seq[Double] = {
    Array.fill(X.length)(X(Random.nextInt(X.length)))
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

  // private
  def initialConfidence: Double = Math.max(q.lhs.confidence, q.rhs.confidence)
  def adjustedConfidence(round: Int): Double = bonferroni_confidence(initialConfidence, round)

  override def done(round: Int, backend: AutomanAdapter): Boolean = {
    val outcome = metaAnswer(round, backend)
    outcome match {
      case e:Estimate =>
        q.confidence_interval match {
          case UnconstrainedCI() => true
          case SymmetricCI(err) =>
            ((e.value - e.low) < err) &&
            ((e.high - e.value) < err)
          case AsymmetricCI(lerr, herr) =>
            ((e.value - e.low) < lerr) &&
            ((e.high - e.value) < herr)
        }
      case _ => true
    }
  }
}
