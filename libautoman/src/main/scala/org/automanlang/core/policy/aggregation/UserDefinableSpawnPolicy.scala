package org.automanlang.core.policy.aggregation

case class UserDefinableSpawnPolicy(num: Int) extends MinimumSpawnPolicy {
  def min : Int = num
}
