package org.automanlang.core.policy.aggregation

import org.automanlang.core.AutomanAdapter
import org.automanlang.core.answer.AbstractAnswer
import org.automanlang.core.AutomanAdapter

trait MetaAggregationPolicy {
  type A <: Any
  type AA <: AbstractAnswer[A]

  def metaAnswer(round: Int, backend: AutomanAdapter) : AA
  def done(round: Int, backend: AutomanAdapter) : Boolean
}
