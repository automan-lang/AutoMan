package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.policy._
import edu.umass.cs.automan.core.answer.{OverBudgetEstimate, LowConfidenceEstimate, Estimate}
import edu.umass.cs.automan.core.logging.{LogType, LogLevelInfo, DebugLog}
import edu.umass.cs.automan.core.question.confidence._
import edu.umass.cs.automan.core.question.{Response, EstimationQuestion, Question}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}
import scala.util.Random

class BootstrapEstimationPolicy(question: EstimationQuestion)
  extends AggregationPolicy(question) {

  val NumBootstraps = 512

  // cache to ensure that output matches test since the
  // bootstrap is a randomized algorithm
  var bootCache = Map[(Seq[Double],Int,Double),(Double,Double,Double)]()

  DebugLog("Policy: bootstrap estimation (1-dimensional)",LogLevelInfo(),LogType.STRATEGY, question.id)

  /**
    * PRIVATE METHODS
    */

  /**
    * Calculates the best estimate so far, returning the answer, confidence bounds, cost, and confidence level.
    * @param tasks The tasks
    * @param num_comparisons The number of times is_done has been called, inclusive.
    * @return Tuple (estimate, low CI bound, high CI bound, cost, confidence)
    */
  private def answer_selector(tasks: List[Task], num_comparisons: Int): (Double, Double, Double, BigDecimal, Double, Array[Response[Double]]) = {
    val valid_tasks = completed_workerunique_tasks(tasks)

    // extract responses & cast to Double
    // (EstimationQuestion#A is guaranteed to be Double)
    val X = valid_tasks.flatMap(_.answer).asInstanceOf[List[Double]]

    // calculate alpha, with Bonferroni correction
    val adj_conf = bonferroni_confidence(question.confidence, num_comparisons)
    val alpha = 1 - adj_conf

    // do bootstrap
    val (low, est, high) = bootstrap(question.estimator, X, NumBootstraps, alpha)

    // cost
    val cost = valid_tasks.filter { t => t.answer.isDefined && !t.from_memo }.map(_.cost).sum

    // distribution
    val dist = getDistribution(tasks)

    (est, low, high, cost, adj_conf, dist.asInstanceOf[Array[Response[Double]]])
  }

  /**
    * Computes statistic and confidence intervals specified.
    * @param statistic An arbitrary function of the data.
    * @param X an input vector
    * @param B the number of bootstrap replications to perform
    * @param alpha the margin of error
    * @return
    */
  def bootstrap(statistic: Seq[Double] => Double, X: Seq[Double], B: Int, alpha: Double) : (Double,Double,Double) = {
    // check cache
    if (bootCache.contains((X, B, alpha))) {
      bootCache((X, B, alpha))
    } else {
      // compute statistic
      val theta_hat = statistic(X)

      // compute bootstrap replications
      val replications = (1 to B).map { b => statistic(resampleWithReplacement(X)) }

      // compute lower bound
      val low = cdfInverse(alpha / 2.0, replications)

      // compute upper bound
      val high = cdfInverse(1.0 - (alpha / 2.0), replications)

      // cache result
      bootCache += ((X, B, alpha) -> (low, theta_hat, high))

      // return
      (low, theta_hat, high)
    }
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

  def hasUnmarkedDuplicate(tasks: List[Task]) : Boolean = {
    tasks
      .filter(t => t.state == SchedulerState.ACCEPTED || t.state == SchedulerState.ANSWERED)
      .groupBy(t => t.worker_id)
      .foldLeft(false){ case (acc, (wrk_opt, ts)) =>
          wrk_opt match {
            case None => throw new Exception("ACCEPTED and ANSWERED tasks must have associated worker IDs.")
            case Some(w) => acc || (ts.size > 1)
          }
      }
  }

  /**
    * Calculate the number of new tasks to schedule.
    * @param tasks
    * @param num_comparisons
    * @param reward
    * @return
    */
  protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal) : Int = {
    // duplicates should already be marked as dupes here from the list of Tasks
    assert(!hasUnmarkedDuplicate(tasks))

    val answered_no_dupes = tasks.count(t => t.state == SchedulerState.ANSWERED)

    // determine current round
    val cRound = currentRound(tasks)

    // calculate the new total sample size (just doubles the total in every round)
    val ss_tot = question.default_sample_size << cRound

    // minus the number of non-duplicate answers received
    ss_tot - answered_no_dupes
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
    * IMPLEMENTATIONS
    */

  /**
    * Returns true if the strategy has enough data to stop scheduling work.
    * @param tasks The complete list of scheduled tasks.
    * @param num_comparisons The number of times this function has been called, inclusive.
    * @return true iff done.
    */
  override def is_done(tasks: List[Task], num_comparisons: Int): (Boolean,Int) = {
    // if there are SOME completed tasks and
    // our sample size is at least the initial size requested
    if (completed_workerunique_tasks(tasks).nonEmpty &&
        completed_workerunique_tasks(tasks).size >= 12) {
      val done = answer_selector(tasks, num_comparisons) match {
        case (est, low, high, cost, conf, dist) =>
          question.confidence_interval match {
            case UnconstrainedCI() =>
              completed_workerunique_tasks(tasks).size ==
              question.default_sample_size
            case SymmetricCI(err) =>
              ((est - low) < err) &&
              ((high - est) < err)
            case AsymmetricCI(lerr, herr) =>
              ((est - low) < lerr) &&
              ((high - est) < herr)
           }
      }
      // bump comparisons
      (done, num_comparisons + 1)
    // otherwise, wait
    } else {
      // do not bump comparisons
      (false, num_comparisons)
    }
  }

  override def rejection_response(tasks: List[Task]): String =
    "Your answer is incorrect.  " +
      "We value your feedback, so if you think that we are in error, please contact us."

  override def select_answer(tasks: List[Task], num_comparisons: Int): Question#AA = {
    answer_selector(tasks, num_comparisons) match { case (est, low, high, cost, conf, dist) =>
      DebugLog("Estimate is " + low + " ≤ " + est + " ≤ " + high,
        LogLevelInfo(),
        LogType.STRATEGY,
        question.id
      )
      Estimate(est, low, high, cost, conf, question, dist).asInstanceOf[Question#AA]
    }
  }

  override def select_over_budget_answer(tasks: List[Task],
                                         need: BigDecimal,
                                         have: BigDecimal,
                                         num_comparisons: Int): Question#AA = {
    // if we've never scheduled anything,
    // there will be no largest group
    if(completed_workerunique_tasks(tasks).isEmpty) {
      OverBudgetEstimate(need, have, question).asInstanceOf[Question#AA]
    } else {
      answer_selector(tasks, num_comparisons) match {
        case (est, low, high, cost, conf, dist) =>
          DebugLog("Over budget.  Best estimate so far is " + low + " ≤ " + est + " ≤ " + high,
            LogLevelInfo(),
            LogType.STRATEGY,
            question.id)
          LowConfidenceEstimate(est, low, high, cost, conf, question, dist).asInstanceOf[Question#AA]
      }
    }
  }

  // by default, we just accept everything
  override def tasks_to_accept(tasks: List[Task]): List[Task] = {
    val cancels = tasks_to_cancel(tasks).toSet
    tasks.filter { t =>
      not_final(t) &&
      !cancels.contains(t)
    }
  }

  // by default, we reject nothing
  override def tasks_to_reject(tasks: List[Task]): List[Task] = Nil
}
