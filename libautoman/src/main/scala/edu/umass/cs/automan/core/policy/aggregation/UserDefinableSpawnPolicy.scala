package edu.umass.cs.automan.core.policy.aggregation

case class UserDefinableSpawnPolicy(num: Int) extends MinimumSpawnPolicy {
  def min : Int = num
}
