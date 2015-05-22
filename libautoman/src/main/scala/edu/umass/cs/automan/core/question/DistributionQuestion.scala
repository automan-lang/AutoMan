package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.policy.price.FixedPricePolicy
import edu.umass.cs.automan.core.policy.timeout.FixedTimeoutPolicy
import edu.umass.cs.automan.core.policy.validation.DefaultDistributionPolicy

abstract class DistributionQuestion extends Question {
  type AA <: AbstractVectorAnswer[A]
  type O <: DistributionOutcome[A]
  type VS = DefaultDistributionPolicy
  type PS = FixedPricePolicy
  type TS = FixedTimeoutPolicy

  private var _sample_size: Int = 30

  def sample_size_=(n: Int) { _sample_size = n }
  def sample_size : Int = _sample_size
}
