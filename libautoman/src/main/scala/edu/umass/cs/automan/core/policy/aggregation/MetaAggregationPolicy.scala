package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.AbstractAnswer

trait MetaAggregationPolicy {
  type A <: Any
  type AA <: AbstractAnswer[A]

  def metaAnswer(round: Int, backend: AutomanAdapter) : AA
  def done(round: Int, backend: AutomanAdapter) : Boolean
}
