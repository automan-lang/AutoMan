package edu.umass.cs.automan.core

import edu.umass.cs.automan.core.policy.aggregation.{MinimumSpawnPolicy, UserDefinableSpawnPolicy}

object MagicNumbers {
  val USFederalMinimumWage : BigDecimal = 7.25 // per hour
  val UpdateFrequencyMs: Int = 30000
  val QuestionTimeoutMultiplier: Double = 500
  val InitialWorkerTimeoutInS: Int = 30
  val DefaultBudget: BigDecimal = 5.00
  val DefaultSampleSizeForDistrib: Int = 30
  val DefaultConfidence: Double = 0.95
  val DefaultSpawnPolicy: MinimumSpawnPolicy = UserDefinableSpawnPolicy(0)
  val DefaultCPC: BigDecimal = 0.10
}
