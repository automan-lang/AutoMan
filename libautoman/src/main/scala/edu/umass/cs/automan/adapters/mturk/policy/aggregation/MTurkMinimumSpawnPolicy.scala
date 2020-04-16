package edu.umass.cs.automan.adapters.mturk.policy.aggregation

import edu.umass.cs.automan.core.policy.aggregation.MinimumSpawnPolicy

object MTurkMinimumSpawnPolicy extends MinimumSpawnPolicy {
  def min: Int = 12
}
