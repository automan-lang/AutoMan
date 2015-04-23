package edu.umass.cs.automan.core.answer

abstract class AbstractAnswer[T](val value: T, val cost: BigDecimal)

sealed abstract class AbstractScalarAnswer[T](value: T, cost: BigDecimal, confidence: Double)
  extends AbstractAnswer[T](value, cost)
case class ScalarAnswer[T](override val value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](value, cost, confidence)
case class ScalarOverBudget[T](override val value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](value, cost, confidence)
