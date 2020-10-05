package org.automanlang.adapters.mturk.policy.aggregation

import org.automanlang.core.policy.aggregation.MinimumSpawnPolicy

object MTurkMinimumSpawnPolicy extends MinimumSpawnPolicy {
  def min: Int = 12
}
