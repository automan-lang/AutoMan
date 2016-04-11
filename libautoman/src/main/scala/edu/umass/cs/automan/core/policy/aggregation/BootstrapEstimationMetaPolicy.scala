package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.answer.AbstractEstimate
import edu.umass.cs.automan.core.question.EstimationQuestion

class BootstrapEstimationMetaPolicy(lhs: EstimationQuestion, rhs: EstimationQuestion, op: Double => Double => Double) extends MetaAggregationPolicy {
  override type A = Double
  override type AA = AbstractEstimate

  def computeAnswer(round: Int): AA = {
    // get adjusted confidence
    val conf = adjustedConfidence(round)

    // get per-question confidence
    val perQConf = Math.sqrt(conf)

    // create and run new questions with higher confidence levels
    val lhs2 = lhs.cloneWithConfidence(perQConf)
    val rhs2 = rhs.cloneWithConfidence(perQConf)

    // compute estimate


    // compute bounds

    // return answer

    ???
  }

  def done: Boolean = ???

  // private
  def initialConfidence: Double = Math.max(lhs.confidence, rhs.confidence)
  def adjustedConfidence(round: Int): Double = bonferroni_confidence(initialConfidence, round)
}
