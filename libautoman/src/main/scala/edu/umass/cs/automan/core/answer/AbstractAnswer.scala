package edu.umass.cs.automan.core.answer

import java.util.UUID

import edu.umass.cs.automan.core.question.Response

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
abstract class AbstractAnswer[T](val cost: BigDecimal, val question_id: UUID, val distribution: Array[Response[T]])

sealed abstract class AbstractMultiEstimate(cost: BigDecimal, question_id: UUID, distribution: Array[Response[Array[Double]]])
  extends AbstractAnswer[Array[Double]](cost, question_id, distribution)
sealed abstract class AbstractEstimate(cost: BigDecimal, question_id: UUID, distribution: Array[Response[Double]])
  extends AbstractAnswer[Double](cost, question_id, distribution)
sealed abstract class AbstractScalarAnswer[T](cost: BigDecimal, question_id: UUID, distribution: Array[Response[T]])
  extends AbstractAnswer[T](cost, question_id, distribution)
sealed abstract class AbstractVectorAnswer[T](cost: BigDecimal, question_id: UUID, distribution: Array[Response[T]])
  extends AbstractAnswer[T](cost, question_id, distribution)

case class MultiEstimate(values: Array[Double],
                         lows: Array[Double],
                         highs: Array[Double],
                         override val cost: BigDecimal,
                         confidence: Double,
                         override val question_id: UUID,
                         override val distribution: Array[Response[Array[Double]]])
  extends AbstractMultiEstimate(cost, question_id, distribution)
case class LowConfidenceMultiEstimate(values: Array[Double],
                                      lows: Array[Double],
                                      highs: Array[Double],
                                      override val cost: BigDecimal,
                                      confidence: Double,
                                      override val question_id: UUID,
                                      override val distribution: Array[Response[Array[Double]]])
  extends AbstractMultiEstimate(cost, question_id, distribution)
case class OverBudgetMultiEstimate(need: BigDecimal, have: BigDecimal, override val question_id: UUID)
  extends AbstractMultiEstimate(need, question_id, Array())

case class Estimate(value: Double,
                    low: Double,
                    high: Double,
                    override val cost: BigDecimal,
                    confidence: Double,
                    override val question_id: UUID,
                    override val distribution: Array[Response[Double]])
  extends AbstractEstimate(cost, question_id, distribution)
case class LowConfidenceEstimate(value: Double,
                                 low: Double,
                                 high: Double,
                                 override val cost: BigDecimal,
                                 confidence: Double,
                                 override val question_id: UUID,
                                 override val distribution: Array[Response[Double]])
  extends AbstractEstimate(cost, question_id, distribution)
case class OverBudgetEstimate(need: BigDecimal, have: BigDecimal, override val question_id: UUID)
  extends AbstractEstimate(need, question_id, Array())

case class Answer[T](value: T,
                     override val cost: BigDecimal,
                     confidence: Double,
                     override val question_id: UUID,
                     override val distribution: Array[Response[T]])
  extends AbstractScalarAnswer[T](cost, question_id, distribution)
case class LowConfidenceAnswer[T](value: T,
                                  override val cost: BigDecimal,
                                  confidence: Double,
                                  override val question_id: UUID,
                                  override val distribution: Array[Response[T]])
  extends AbstractScalarAnswer[T](cost, question_id, distribution)
case class OverBudgetAnswer[T](need: BigDecimal, have: BigDecimal, override val question_id: UUID)
  extends AbstractScalarAnswer[T](need, question_id, Array())

case class Answers[T](values: Set[(String,T)],
                      override val cost: BigDecimal,
                      override val question_id: UUID,
                      override val distribution: Array[Response[T]])
  extends AbstractVectorAnswer[T](cost, question_id, distribution)
case class IncompleteAnswers[T](values: Set[(String,T)],
                                override val cost: BigDecimal,
                                override val question_id: UUID,
                                override val distribution: Array[Response[T]])
  extends AbstractVectorAnswer[T](cost, question_id, distribution)
case class OverBudgetAnswers[T](need: BigDecimal,
                                have: BigDecimal,
                                override val question_id: UUID)
  extends AbstractScalarAnswer[T](need, question_id, Array())