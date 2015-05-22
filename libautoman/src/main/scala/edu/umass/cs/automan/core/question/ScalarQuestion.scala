package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.{ScalarOutcome, AbstractScalarAnswer}
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy
import edu.umass.cs.automan.core.policy.validation.DefaultScalarPolicy

abstract class ScalarQuestion extends Question {
  type AA = AbstractScalarAnswer[A]
  type O = ScalarOutcome[A]
  type VS = DefaultScalarPolicy
  type PS = MLEPricePolicy
  type TS = DoublingTimeoutPolicy

  protected var _confidence: Double = 0.95

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence
}
