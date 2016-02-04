package edu.umass.cs.automan.core.answer

/**
 * Most abstract answer type.  Subtypes are scalar and
 * vectors answers.  The three subtypes exist to make
 * pattern matching on result types simpler for
 * programmers; it does not make sense to expect
 * a vector of responses for questions that can only
 * return a scalar.  Abstract types are sealed so that
 * the compiler can warn the user about incomplete cases.
 * @param cost Cost of the answer returned.
 * @tparam T The type of the enclosed answer.
 */
abstract class AbstractAnswer[T](val cost: BigDecimal)

sealed abstract class AbstractEstimate(cost: BigDecimal)
  extends AbstractAnswer[Double](cost)
sealed abstract class AbstractScalarAnswer[T](cost: BigDecimal)
  extends AbstractAnswer[T](cost)
sealed abstract class AbstractVectorAnswer[T](cost: BigDecimal)
  extends AbstractAnswer[T](cost)

case class Estimate(value: Double, low: Double, high: Double, override val cost: BigDecimal, confidence: Double)
  extends AbstractEstimate(cost)
case class LowConfidenceEstimate(value: Double, low: Double, high: Double, override val cost: BigDecimal, confidence: Double)
  extends AbstractEstimate(cost)
case class OverBudgetEstimate(need: BigDecimal, have: BigDecimal) extends AbstractEstimate(need)

case class Answer[T](value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](cost)
case class LowConfidenceAnswer[T](value: T, override val cost: BigDecimal, confidence: Double)
  extends AbstractScalarAnswer[T](cost)
case class OverBudgetAnswer[T](need: BigDecimal, have: BigDecimal) extends AbstractScalarAnswer[T](need)

case class Answers[T](values: Set[(String,T)], override val cost: BigDecimal)
  extends AbstractVectorAnswer[T](cost)
case class IncompleteAnswers[T](values: Set[(String,T)], override val cost: BigDecimal)
  extends AbstractVectorAnswer[T](cost)
case class OverBudgetAnswers[T](need: BigDecimal, have: BigDecimal) extends AbstractScalarAnswer[T](need)