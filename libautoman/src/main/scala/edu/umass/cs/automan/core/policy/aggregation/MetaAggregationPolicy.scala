package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.answer.AbstractAnswer

trait MetaAggregationPolicy {
  type A <: Any
  type AA <: AbstractAnswer[A]

  def computeAnswer(round: Int) : AA
  def done: Boolean
}
