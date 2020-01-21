package edu.umass.cs.automan.core.answer

import java.util.UUID

import edu.umass.cs.automan.core.question._

/**
 * Most abstract answer type.  Subtypes exist to make
 * pattern matching on result types simpler for
 * programmers. Abstract types are sealed so that
 * the compiler can warn the user about incomplete
 * pattern matches.
 * @param cost Cost of the answer returned.
 * @tparam T The type of the enclosed answer.
 */
abstract class AbstractAnswer[T](val cost: BigDecimal, val question: Question, val distribution: Array[Response[T]])

sealed abstract class AbstractMultiEstimate(cost: BigDecimal, question: MultiEstimationQuestion, distribution: Array[Response[Array[Double]]])
  extends AbstractAnswer[Array[Double]](cost, question, distribution)
sealed abstract class AbstractEstimate(cost: BigDecimal, question: EstimationQuestion, distribution: Array[Response[Double]])
  extends AbstractAnswer[Double](cost, question, distribution)

sealed abstract class AbstractScalarAnswer[T](cost: BigDecimal, question: DiscreteScalarQuestion, distribution: Array[Response[T]])
  extends AbstractAnswer[T](cost, question, distribution)
sealed abstract class AbstractVectorAnswer[T](cost: BigDecimal, question: VectorQuestion, distribution: Array[Response[T]])
  extends AbstractAnswer[T](cost, question, distribution)
sealed abstract class AbstractSurveyAnswer(cost: BigDecimal, survey: Survey, distribution: Array[Response[(String,Question#A)]])
  extends AbstractAnswer[(String,Question#A)](cost, survey, distribution)

case class MultiEstimate(values: Array[Double],
                         lows: Array[Double],
                         highs: Array[Double],
                         override val cost: BigDecimal,
                         confidence: Double,
                         override val question: MultiEstimationQuestion,
                         override val distribution: Array[Response[Array[Double]]])
  extends AbstractMultiEstimate(cost, question, distribution)
/**
  * For MultiEstimates in Surveys. Should do nothing.
  */
case class NoMultiEstimate(override val question: MultiEstimationQuestion)
  extends AbstractMultiEstimate(0, question, Array[Response[Array[Double]]]())
case class LowConfidenceMultiEstimate(values: Array[Double],
                                      lows: Array[Double],
                                      highs: Array[Double],
                                      override val cost: BigDecimal,
                                      confidence: Double,
                                      override val question: MultiEstimationQuestion,
                                      override val distribution: Array[Response[Array[Double]]])
  extends AbstractMultiEstimate(cost, question, distribution)
case class OverBudgetMultiEstimate(need: BigDecimal, have: BigDecimal, override val question: MultiEstimationQuestion)
  extends AbstractMultiEstimate(need, question, Array())

case class Estimate(value: Double,
                    low: Double,
                    high: Double,
                    override val cost: BigDecimal,
                    confidence: Double,
                    override val question: EstimationQuestion,
                    override val distribution: Array[Response[Double]])
  extends AbstractEstimate(cost, question, distribution)
/**
  * For Estimates in Surveys. Should do nothing.
  */
case class NoEstimate(override val question: EstimationQuestion)
  extends AbstractEstimate(0, question, Array[Response[Double]]())
case class LowConfidenceEstimate(value: Double,
                                 low: Double,
                                 high: Double,
                                 override val cost: BigDecimal,
                                 confidence: Double,
                                 override val question: EstimationQuestion,
                                 override val distribution: Array[Response[Double]])
  extends AbstractEstimate(cost, question, distribution)
case class OverBudgetEstimate(need: BigDecimal, have: BigDecimal, override val question: EstimationQuestion)
  extends AbstractEstimate(need, question, Array())

case class Answer[T](value: T,
                     override val cost: BigDecimal,
                     confidence: Double,
                     override val question: DiscreteScalarQuestion,
                     override val distribution: Array[Response[T]])
  extends AbstractScalarAnswer[T](cost, question, distribution)

/**
  * For Questions in Surveys. Should do nothing.
  */
case class NoAnswer[T](override val question: DiscreteScalarQuestion)
  extends AbstractScalarAnswer[T](0, question, Array[Response[T]]())
case class LowConfidenceAnswer[T](value: T,
                                  override val cost: BigDecimal,
                                  confidence: Double,
                                  override val question: DiscreteScalarQuestion,
                                  override val distribution: Array[Response[T]])
  extends AbstractScalarAnswer[T](cost, question, distribution)
case class OverBudgetAnswer[T](need: BigDecimal, have: BigDecimal, override val question: DiscreteScalarQuestion)
  extends AbstractScalarAnswer[T](need, question, Array())

case class Answers[T](values: Set[(String,T)], // set of vector answers
                      override val cost: BigDecimal,
                      override val question: VectorQuestion,
                      override val distribution: Array[Response[T]])
  extends AbstractVectorAnswer[T](cost, question, distribution)
/**
  * For Questions in Surveys. Should do nothing.
  */
case class NoAnswers[T](override val question: VectorQuestion)
  extends AbstractVectorAnswer[T](0, question, Array[Response[T]]())

case class SurveyAnswers(values: Set[Map[String,Question#A]], // final dist (no worker ids)
                         override val cost: BigDecimal,
                         override val question: Survey,
                         override val distribution: Array[Response[(String,Question#A)]]) // raw dist
  extends AbstractSurveyAnswer(cost, question, distribution)
case class NoSurveyAnswers(override val question: Survey) // raw dist
  extends AbstractSurveyAnswer(0, question, Array[Response[(String,Question#A)]]())
case class IncompleteAnswers[T](values: Set[(String,T)],
                                override val cost: BigDecimal,
                                override val question: VectorQuestion,
                                override val distribution: Array[Response[T]])
  extends AbstractVectorAnswer[T](cost, question, distribution)
case class OverBudgetAnswers[T](need: BigDecimal,
                                have: BigDecimal,
                                override val question: VectorQuestion)
  extends AbstractVectorAnswer[T](need, question, Array())