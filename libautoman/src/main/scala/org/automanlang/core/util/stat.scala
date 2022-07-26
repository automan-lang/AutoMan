package org.automanlang.core.util

/**
 * Now that we've imported apache-common math. Should use that instead.
 * However, these codes are kept just in case.
 */
@Deprecated
object stat {
  def variance(samples: Array[Double]): Double = {
    val mean = samples.sum / samples.length
    samples.map(t => math.pow(t - mean, 2)).sum / samples.length
  }

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
  def df(v1: Double, v2: Double, sample_size: Int): Double = {
    val vn1 = v1 / sample_size
    val vn2 = v2 / sample_size

    math.pow(vn1 + vn2, 2) / ((math.pow(vn1, 2) + math.pow(vn2, 2)) / (sample_size - 1))
  }
}
