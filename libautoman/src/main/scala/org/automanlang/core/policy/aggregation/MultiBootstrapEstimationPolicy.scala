package org.automanlang.core.policy.aggregation

import org.automanlang.core.policy._
import org.automanlang.core.answer._
import org.automanlang.core.logging.{LogType, LogLevelInfo, DebugLog}
import org.automanlang.core.question.confidence._
import org.automanlang.core.question.{Response, MultiEstimationQuestion, Question}
import org.automanlang.core.scheduler.{SchedulerState, Task}
import scala.util.Random

class MultiBootstrapEstimationPolicy(question: MultiEstimationQuestion)
  extends AggregationPolicy(question) {

  val NumBootstraps = 512 * question.cardinality

  // cache to ensure that output matches test since the
  // bootstrap is a randomized algorithm
  var bootCache = Map[(Seq[Seq[Double]],Int,Double),(Array[Double],Array[Double],Array[Double])]()

  DebugLog("Policy: bootstrap estimation (" + question.cardinality + "-dimensional)",LogLevelInfo(),LogType.STRATEGY, question.id)

  /**
    * PRIVATE METHODS
    */

  /**
    * Calculates the best estimate so far, returning the answer, confidence bounds, cost, and confidence level.
    * @param tasks The tasks
    * @param num_comparisons The number of times is_done has been called, inclusive.
    * @return Tuple (estimate, low CI bound, high CI bound, cost, confidence)
    */
  private def answer_selector(tasks: List[Task], num_comparisons: Int): (Array[Double], Array[Double], Array[Double], BigDecimal, Double, Array[Response[Array[Double]]]) = {
    val valid_tasks = completed_workerunique_tasks(tasks)

    // extract responses & cast to Double
    // (EstimationQuestion#A is guaranteed to be Double)
    val X: List[Array[Double]] = valid_tasks.flatMap(_.answer).asInstanceOf[List[Array[Double]]]

    // calculate alpha, with Bonferroni correction
    val adj_conf = bonferroni_confidence(question.confidence, num_comparisons)
    val alpha = 1 - adj_conf

    // do bootstrap
    val (lows, ests, highs) = bootstrap(question.estimator, X, NumBootstraps, alpha)

    // cost
    val cost = valid_tasks.filter { t => t.answer.isDefined && !t.from_memo }.map(_.cost).sum

    // distribution
    val dist = getDistribution(tasks)

    (ests, lows, highs, cost, adj_conf, dist.asInstanceOf[Array[Response[Array[Double]]]])
  }

  /**
    * Computes statistic and confidence intervals specified.
    * @param statistic An arbitrary function of the data.
    * @param X an input matrix
    * @param B the number of bootstrap replications to perform
    * @param alpha the margin of error
    * @return
    */
  private def bootstrap(statistic: Seq[Array[Double]] => Array[Double],
                        X: Seq[Array[Double]],
                        B: Int,
                        alpha: Double) : (Array[Double],Array[Double],Array[Double]) = {
    // Convert Arrays to Seqs; equivalent arrays
    // are not equal in the JVM so they do not
    // work correctly as Map keys.
    val X2 = X.map(_.toSeq)

    if (bootCache.contains((X2,B,alpha))) {
      bootCache(X2,B,alpha)
    } else {
      // compute statistics
      val theta_hats = statistic(X)

      // compute bootstrap replications
      val replications = (1 to B).map { b => statistic(resampleWithReplacement(X)) }

      val repSlice = slice(replications)

      // compute lower bounds
      val lows = repSlice.map { rep => cdfInverse(alpha / 2.0, rep) }.toArray

      // compute upper bounds
      val highs = repSlice.map { rep => cdfInverse(1.0 - (alpha / 2.0), rep) }.toArray

      assert(lows.length == question.cardinality)
      assert(theta_hats.length == question.cardinality)
      assert(highs.length == question.cardinality)

      // put in cache
      bootCache += ((X2,B,alpha) -> (lows, theta_hats, highs))

      // return
      (lows, theta_hats, highs)
    }
  }

  def slice(replications: Seq[Array[Double]]) : Seq[Array[Double]] = {
    (0 until question.cardinality).map { dim =>
      replications.map(_(dim)).toArray
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
  private def resampleWithReplacement(X: Seq[Array[Double]]) : Seq[Array[Double]] = {
    X.map {
      x => Array.fill(x.length)(x(Random.nextInt(x.length)))
    }
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
        case (ests, lows, highs, cost, conf, dist) =>
          ests.indices.foldLeft(true) { case (acc,i) =>
            question.confidence_region(i) match {
              case UnconstrainedCI() =>
                acc &&
                  completed_workerunique_tasks(tasks).size ==
                  question.default_sample_size
              case SymmetricCI(err) =>
                acc &&
                  ((ests(i) - lows(i)) < err) &&
                  ((highs(i) - ests(i)) < err)
              case AsymmetricCI(lerr, herr) =>
                acc &&
                  ((ests(i) - lows(i)) < lerr) &&
                  ((highs(i) - ests(i)) < herr)
            }
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
    answer_selector(tasks, num_comparisons) match { case (ests, lows, highs, cost, conf, dist) =>
      ests.indices.foreach { i =>
        DebugLog("Estimate is " + lows(i) + " ≤ " + ests(i) + " ≤ " + highs(i),
          LogLevelInfo(),
          LogType.STRATEGY,
          question.id
        )
      }
      MultiEstimate(ests, lows, highs, cost, conf, question, dist).asInstanceOf[Question#AA]
    }
  }

  override def select_over_budget_answer(tasks: List[Task],
                                         need: BigDecimal,
                                         have: BigDecimal,
                                         num_comparisons: Int): Question#AA = {
    // if we've never scheduled anything,
    // there will be no largest group
    if(completed_workerunique_tasks(tasks).isEmpty) {
      OverBudgetMultiEstimate(need, have, question).asInstanceOf[Question#AA]
    } else {
      answer_selector(tasks, num_comparisons) match {
        case (ests, lows, highs, cost, conf, dist) =>
          ests.indices.foreach { i =>
            DebugLog("Over budget.  Best estimate so far is " + lows(i) + " ≤ " + ests(i) + " ≤ " + highs(i),
              LogLevelInfo(),
              LogType.STRATEGY,
              question.id)
          }
          LowConfidenceMultiEstimate(ests, lows, highs, cost, conf, question, dist).asInstanceOf[Question#AA]
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
