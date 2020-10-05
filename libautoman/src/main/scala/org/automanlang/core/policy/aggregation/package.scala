package org.automanlang.core.policy

package object aggregation {
  /**
    * Find the multiple-comparisons-adjusted confidence level
    * using the Bonferroni-Holm adjustment.
    * @param confidence The unadjusted confidence level.
    * @param num_comparisons The number of comparisons.
    * @return The adjusted confidence level.
    */
  protected[policy] def bonferroni_confidence(confidence: Double, num_comparisons: Int) : Double = {
    assert(num_comparisons > 0)
    1 - (1 - confidence) / num_comparisons.toDouble
  }
}
