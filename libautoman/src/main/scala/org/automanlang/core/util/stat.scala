package org.automanlang.core.util

import org.apache.commons.math3.stat.StatUtils

/**
 * Now that we've imported apache-common math. Many of the following functions
 * are no longer needed. We should use apache-common math if possible.
 * However, these codes are marked as deprecated and kept just in case.
 */
object stat {
  def mean(samples: Array[Double]): Double = {
    val mean = samples.sum / samples.length
    mean
  }

  def variance(samples: Array[Double]): Double = {
    val meanV = mean(samples)
    samples.map(t => math.pow(t - meanV, 2)).sum / (samples.length - 1)
  }

  @Deprecated
  def standardDeviation(samples: Array[Double]): Double = math.sqrt(variance(samples))

  /**
   * Performs Welch's unequal variances t-Test
   * https://github.com/scipy/scipy/blob/v1.8.1/scipy/stats/_stats_py.py#L6213-L6215
   *
   * @param m1          mean for first sample
   * @param m2          mean for second sample
   * @param v1          variance for first sample
   * @param v2          variance for second sample
   * @param sample_size size for both samples
   * @return
   */
  @Deprecated
  def unequalT(m1: Double, m2: Double, v1: Double, v2: Double, sample_size: Int): Double = {
    (m1 - m2) / math.sqrt((v1 + v2) / sample_size)
  }

  /**
   * Use Welch–Satterthwaite equation to estimate degrees of freedom.
   *
   * Reference: https://github.com/scipy/scipy/blob/v1.8.1/scipy/stats/_stats_py.py#L5752-L5762
   *
   * @param v1          variance for first sample
   * @param v2          variance for second sample
   * @param sample_size size of both samples
   * @return
   */
  @Deprecated
  def df(v1: Double, v2: Double, sample_size: Int): Double = {
    val vn1 = v1 / sample_size
    val vn2 = v2 / sample_size

    math.pow(vn1 + vn2, 2) / ((math.pow(vn1, 2) + math.pow(vn2, 2)) / (sample_size - 1))
  }

  def cohen_d(x: Array[Double], y:Array[Double]): Double = {
    val nx = x.length
    val ny = y.length

    // degree of freedom
    val dof = nx + ny - 2

    // note that StatUtils.variance is error-corrected (subtracting 1 from size)
    val d = (mean(x) - mean(y)) / Math.sqrt(
      ((nx-1) * Math.pow(variance(x), 2) +
        (ny-1) * Math.pow(variance(y), 2)
      ) / dof)
    d
  }
}
