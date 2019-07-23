package edu.umass.cs.automan.adapters.googleads.policy.aggregation

import edu.umass.cs.automan.core.policy.aggregation.MinimumSpawnPolicy

object GMinimumSpawnPolicy extends MinimumSpawnPolicy {
  def min: Int = 10
}
