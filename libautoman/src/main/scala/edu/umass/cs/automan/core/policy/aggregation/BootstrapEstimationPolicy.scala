package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.answer.{LowConfidenceEstimate, LowConfidenceAnswer, OverBudgetAnswer, Estimate}
import edu.umass.cs.automan.core.logging.{LogType, LogLevelInfo, DebugLog}
import edu.umass.cs.automan.core.question.{EstimationQuestion, Question}
import edu.umass.cs.automan.core.scheduler.Task

import scala.util.Random

class BootstrapEstimationPolicy(estimator: Seq[Double] => Double, ci_width: Double, question: EstimationQuestion)
  extends ScalarPolicy(question) {

  val NumBootstraps = 512

  DebugLog("Policy: bootstrap estimation",LogLevelInfo(),LogType.STRATEGY, question.id)

  /**
    * PRIVATE METHODS
    */

  /**
    * Computes statistic and confidence intervals specified.
    * @param statistic An arbitrary function of the data.
    * @param X an input vector
    * @param B the number of bootstrap replications to perform
    * @param alpha the margin of error
    * @return
    */
  private def bootstrap(statistic: Seq[Double] => Double, X: Seq[Double], B: Int, alpha: Double) : (Double,Double,Double) = {
    // compute statistic
    val theta_hat = statistic(X)

    // compute bootstrap replications
    val replications = (1 to B).map { b => statistic(resampleWithReplacement(X)) }

    // compute lower bound
    val low = cdfInverse(alpha / 2.0, replications)

    // compute upper bound
    val high = cdfInverse(1.0 - (alpha / 2.0), replications)

    // return
    (low, theta_hat, high)
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

  /**
    * Returns an equal-length vector X' formed from resampling with
    * replacement from X with uniform probability.
    * @param X input data vector
    * @return X'
    */
  private def resampleWithReplacement(X: Seq[Double]) : Seq[Double] = {
    Array.fill(X.length)(X(Random.nextInt(X.length)))
  }

  private def iterate(N: Int, width: Double, B: Int, alpha: Double, X: Seq[Double], fn: Seq[Double] => Double) : (Double,Double,Double,Int) = {
    var X_prime = X
    var n = N / 2
    var high = Double.MaxValue
    var low = Double.MinValue
    var est = 0.0
    while (high - low > width) {
      n *= 2
      // double length and keep going
      if (n > X_prime.length) {
        X_prime = Seq.concat(X_prime, resampleWithReplacement(X_prime))
      }
      val (l, e, h) = bootstrap(fn, X_prime.take(n), B, alpha)
      high = h
      low = l
      est = e
    }

    (high,low,est,n)
  }

  /**
    * Calculates the best estimate so far, returning the answer, confidence bounds, cost, and confidence level.
    * @param tasks The tasks
    * @return Tuple (estimate, low CI bound, high CI bound, cost, confidence)
    */
  private def answer_selector(tasks: List[Task]): (Double, Double, Double, BigDecimal, Double) = {
    // extract responses & cast to Double
    // (EstimationQuestion#A is guaranteed to be Double)
    val X = tasks.flatMap(_.answer).asInstanceOf[List[Double]]

    // calculate alpha, with Bonferroni correction
    val adj_conf = bonferroni_confidence(question.confidence, numComparisons(tasks))
    val alpha = 1 - adj_conf

    // do bootstrap
    val (low, est, high) = bootstrap(estimator, X, NumBootstraps, alpha)

    // cost
    val cost = tasks.filter(_.answer.isDefined).map(_.cost).sum

    (est, low, high, cost, adj_conf)
  }

  /**
    * IMPLEMENTATIONS
    */

  override def rejection_response(tasks: List[Task]): String = ???

  override def select_answer(tasks: List[Task]): Question#AA = {
    answer_selector(tasks) match { case (est, low, high, cost, conf) =>
      DebugLog("Estimate is " + low + " ≤ " + est + " ≤ " + high,
        LogLevelInfo(),
        LogType.STRATEGY,
        question.id
      )
      Estimate(est, low, high, cost, conf).asInstanceOf[Question#AA]
    }
  }

  override def select_over_budget_answer(tasks: List[Task],
                                         need: BigDecimal,
                                         have: BigDecimal): Question#AA = {
    // if we've never scheduled anything,
    // there will be no largest group
    if(completed_workerunique_tasks(tasks).isEmpty) {
      OverBudgetAnswer(need, have).asInstanceOf[Question#AA]
    } else {
      answer_selector(tasks) match {
        case (est, low, high, cost, conf) =>
          DebugLog("Over budget.  Best estimate so far is " + low + " ≤ " + est + " ≤ " + high,
            LogLevelInfo(),
            LogType.STRATEGY,
            question.id)
          LowConfidenceEstimate(est, low, high, cost, conf).asInstanceOf[Question#AA]
      }
    }
  }

  // by default, we just accept everything
  override def tasks_to_accept(tasks: List[Task]): List[Task] = tasks

  // by default, we reject nothing
  override def tasks_to_reject(tasks: List[Task]): List[Task] = List.empty

  override def current_confidence(tasks: List[Task]): Double =
    // TODO: funny question, right? Isn't it always question.confidence unless tasks is empty?
    ???

  override def is_confident(tasks: List[Task], num_hypotheses: Int): Boolean = {
    if (tasks.isEmpty) {
      DebugLog("Have no tasks; confidence is undefined.", LogLevelInfo(), LogType.STRATEGY, question.id)
      false
    } else {
      val conf = current_confidence(tasks)
      val thresh = bonferroni_confidence(question.confidence, num_hypotheses)
      if (conf >= thresh) {
        DebugLog("Reached or exceeded alpha = " + (1 - thresh).toString, LogLevelInfo(), LogType.STRATEGY, question.id)
        true
      } else {
        val valid_ts = completed_workerunique_tasks(tasks)
        if (valid_ts.nonEmpty) {
          val biggest_answer = valid_ts.groupBy(_.answer).maxBy{ case(sym,ts) => ts.size }._2.size
          DebugLog("Need more tasks for alpha = " + (1 - thresh) + "; have " + biggest_answer + " agreeing tasks.", LogLevelInfo(), LogType.STRATEGY, question.id)
        } else {
          DebugLog("Need more tasks for alpha = " + (1 - thresh) + "; currently have no agreement.", LogLevelInfo(), LogType.STRATEGY, question.id)
        }
        false
      }
    }

  /**
    * Computes the number of tasks needed to satisfy the quality-control
    * algorithm given the already-collected list of tasks. Returns only
    * newly-created tasks.
    *
    * @param tasks The complete list of previously-scheduled tasks
    * @param suffered_timeout True if any of the latest batch of tasks suffered a timeout.
    * @return A list of new tasks to schedule on the backend.
    */
  override def spawn(tasks: List[Task], suffered_timeout: Boolean): List[Task] = ???
}
