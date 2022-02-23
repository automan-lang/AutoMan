package org.automanlang.core.answer

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import au.com.bytecode.opencsv.CSVWriter
import org.automanlang.core.question._
import scala.collection.JavaConversions._

import scala.collection.mutable.ListBuffer

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
sealed abstract class AbstractMixedAnswer[T](cost: BigDecimal, question: MixedQuestion, distribution: Array[Response[T]])
  extends AbstractAnswer[T](cost, question, distribution)

/**
  * MULTI-ESTIMATES
  */
case class MultiEstimate(values: Array[Double],
                         lows: Array[Double],
                         highs: Array[Double],
                         override val cost: BigDecimal,
                         confidence: Double,
                         override val question: MultiEstimationQuestion,
                         override val distribution: Array[Response[Array[Double]]])
  extends AbstractMultiEstimate(cost, question, distribution)
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

/**
  * ESTIMATES
  */
case class Estimate(value: Double,
                    low: Double,
                    high: Double,
                    override val cost: BigDecimal,
                    confidence: Double,
                    override val question: EstimationQuestion,
                    override val distribution: Array[Response[Double]])
  extends AbstractEstimate(cost, question, distribution)
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

/**
  * SCALARS
  */
case class Answer[T](value: T,
                     override val cost: BigDecimal,
                     confidence: Double,
                     override val question: DiscreteScalarQuestion,
                     override val distribution: Array[Response[T]])
  extends AbstractScalarAnswer[T](cost, question, distribution)
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

/**
  * VECTORS
  */
case class Answers[T](values: Set[(String,T)], // set of vector answers
                      override val cost: BigDecimal,
                      override val question: VectorQuestion,
                      override val distribution: Array[Response[T]])
  extends AbstractVectorAnswer[T](cost, question, distribution)
case class NoAnswers[T](override val question: VectorQuestion)
  extends AbstractVectorAnswer[T](0, question, Array[Response[T]]())
case class IncompleteAnswers[T](values: Set[(String,T)],
                                override val cost: BigDecimal,
                                override val question: VectorQuestion,
                                override val distribution: Array[Response[T]])
  extends AbstractVectorAnswer[T](cost, question, distribution)
case class OverBudgetAnswers[T](need: BigDecimal,
                                have: BigDecimal,
                                override val question: VectorQuestion)
  extends AbstractVectorAnswer[T](need, question, Array())


/**
  *  Mixed
  */
case class AnswersM[T](values: Set[(String,T)], // set of mixed answers
                      override val cost: BigDecimal,
                      override val question: MixedQuestion,
                      override val distribution: Array[Response[T]])
  extends AbstractMixedAnswer[T](cost, question, distribution)
case class NoAnswersM[T](override val question: MixedQuestion)
  extends AbstractMixedAnswer[T](0, question, Array[Response[T]]())
case class IncompleteAnswersM[T](values: Set[(String,T)],
                                override val cost: BigDecimal,
                                override val question: MixedQuestion,
                                override val distribution: Array[Response[T]])
  extends AbstractMixedAnswer[T](cost, question, distribution)
case class OverBudgetAnswersM[T](need: BigDecimal,
                                have: BigDecimal,
                                override val question: MixedQuestion)
  extends AbstractMixedAnswer[T](need, question, Array())