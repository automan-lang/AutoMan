package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.{ScalarOutcome, AbstractScalarAnswer}

abstract class DiscreteScalarQuestion extends Question {
  type AA = AbstractScalarAnswer[A]
  type O = ScalarOutcome[A]

  protected var _confidence: Double = 0.95

  def confidence_=(c: Double) { _confidence = c }
  def confidence: Double = _confidence

  def num_possibilities: BigInt
}
