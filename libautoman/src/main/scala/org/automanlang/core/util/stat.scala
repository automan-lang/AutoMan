package org.automanlang.core.util

object stat {
  def variance(samples: Array[Double]): Double = {
    val mean = samples.sum / samples.length
    samples.map(t => math.pow(t - mean, 2)).sum / samples.length
  }

  def standardDeviation(samples: Array[Double]): Double = math.sqrt(variance(samples))

  def homoscedasticTTest(m1: Double, m2: Double, v1: Double, v2: Double, sample_size: Int): Double = ???
}
