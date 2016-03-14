package edu.umass.cs.automan.adapters.mturk.policy.aggregation

import edu.umass.cs.automan.core.policy.aggregation.MinimumSpawnPolicy

class MTurkMinimumSpawnPolicy extends MinimumSpawnPolicy {
  def min: Int = 10
}
