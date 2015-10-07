package edu.umass.cs.automan.core.answer

/**
 * Most abstract answer type.  Subtypes are scalar and
 * vectors answers.  The two subtypes exist to make
 * pattern matching on result types simpler for
 * programmers; it does not make sense to expect
 * a vector of responses for questions that can only
 * return a scalar.
 * @param cost Cost of the answer returned.
 * @tparam T The type of the enclosed answer.
 */
abstract class AbstractAnswer[T](val cost: BigDecimal)

sealed abstract class AbstractScalarAnswer[T](cost: BigDecimal)
  extends AbstractAnswer[T](cost)
sealed abstract class AbstractVectorAnswer[T](cost: BigDecimal)
  extends AbstractAnswer[T](cost)

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