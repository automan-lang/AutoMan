package edu.umass.cs.automan.core.answer

import java.util.UUID

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
abstract class AbstractAnswer[T](val cost: BigDecimal, val question_id: UUID)

sealed abstract class AbstractEstimate(cost: BigDecimal, question_id: UUID)
  extends AbstractAnswer[Double](cost, question_id)
sealed abstract class AbstractScalarAnswer[T](cost: BigDecimal, question_id: UUID)
  extends AbstractAnswer[T](cost, question_id)
sealed abstract class AbstractVectorAnswer[T](cost: BigDecimal, question_id: UUID)
  extends AbstractAnswer[T](cost, question_id)

case class Estimate(value: Double, low: Double, high: Double, override val cost: BigDecimal, confidence: Double, override val question_id: UUID)
  extends AbstractEstimate(cost, question_id)
case class LowConfidenceEstimate(value: Double, low: Double, high: Double, override val cost: BigDecimal, confidence: Double, override val question_id: UUID)
  extends AbstractEstimate(cost, question_id)
case class OverBudgetEstimate(need: BigDecimal, have: BigDecimal, override val question_id: UUID)
  extends AbstractEstimate(need, question_id)

case class Answer[T](value: T, override val cost: BigDecimal, confidence: Double, override val question_id: UUID)
  extends AbstractScalarAnswer[T](cost, question_id)
case class LowConfidenceAnswer[T](value: T, override val cost: BigDecimal, confidence: Double, override val question_id: UUID)
  extends AbstractScalarAnswer[T](cost, question_id)
case class OverBudgetAnswer[T](need: BigDecimal, have: BigDecimal, override val question_id: UUID)
  extends AbstractScalarAnswer[T](need, question_id)

case class Answers[T](values: Set[(String,T)], override val cost: BigDecimal, override val question_id: UUID)
  extends AbstractVectorAnswer[T](cost, question_id)
case class IncompleteAnswers[T](values: Set[(String,T)], override val cost: BigDecimal, override val question_id: UUID)
  extends AbstractVectorAnswer[T](cost, question_id)
case class OverBudgetAnswers[T](need: BigDecimal, have: BigDecimal, override val question_id: UUID)
  extends AbstractScalarAnswer[T](need, question_id)