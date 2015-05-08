package edu.umass.cs.automan.core.answer

abstract class AbstractAnswer[T](val cost: BigDecimal)

sealed abstract class AbstractScalarAnswer[T](value: T, cost: BigDecimal, confidence: Double)
  extends AbstractAnswer[T](cost)
sealed abstract class AbstractDistributionAnswer[T](values: Set[T], cost: BigDecimal)
  extends AbstractAnswer[T](cost)

case class ScalarAnswer[T](value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](value, cost, confidence)
case class ScalarOverBudget[T](value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](value, cost, confidence)

case class DistributionAnswer[T](values: Set[T], override val cost: BigDecimal)
  extends AbstractDistributionAnswer[T](values, cost)
case class DistributionOverBudget[T](values: Set[T], override val cost: BigDecimal)
  extends AbstractDistributionAnswer[T](values, cost)