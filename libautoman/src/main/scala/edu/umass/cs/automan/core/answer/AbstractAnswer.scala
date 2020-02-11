package edu.umass.cs.automan.core.answer

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
sealed abstract class AbstractSurveyAnswer(cost: BigDecimal, survey: Survey, distribution: Array[Response[Set[(String,Question#A)]]])
  extends AbstractAnswer[Set[(String,Question#A)]](cost, survey, distribution)
//sealed abstract class AbstractGrammarAnswer[T](cost: BigDecimal, question: GrammarQuestion, distribution: Array[Response[T]])
//  extends AbstractAnswer[T](cost, question, distribution)

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
  * SURVEYS
  */
case class SurveyAnswers(values: Set[Map[String,Question#A]], // final dist (no worker ids) // todo values: Set[(String, Question#A)],?
                         override val cost: BigDecimal,
                         override val question: Survey,
                         override val distribution: Array[Response[Set[(String,Question#A)]]])
  extends AbstractSurveyAnswer(cost, question, distribution) {
  override def toString: String = {
    println("Dan is sort of right")
    val s = values.head("")

//    val s: Set[(String, Question#A)] = values.flatMap(m => {
//      m.map {
//        case (key, value) => {
//          (key, value)
//        }
//      }
//    })
//    val s = values.map {
//      case (key -> value) => {
//        (key,value)
//      }
//    }
    question.prettyPrintAnswer(s.asInstanceOf[SurveyAnswers.this.question.A]) // survey A = Set[(String,Question#A)] values.asInstanceOf[this.question.A]
  }
}
case class NoSurveyAnswers(override val question: Survey) // raw dist
  extends AbstractSurveyAnswer(0, question, Array[Response[Set[(String,Question#A)]]]())
case class IncompleteSurveyAnswers[T](values: Set[Map[String,Question#A]],
                                override val cost: BigDecimal,
                                override val question: Survey,
                                override val distribution: Array[Response[Set[(String,Question#A)]]])
  extends AbstractSurveyAnswer(cost, question, distribution)

/**
  * GRAMMAR
  */
//case class GrammarAnswer[T](value: T,
//                     override val cost: BigDecimal,
//                     confidence: Double,
//                     override val question: GrammarQuestion,
//                     override val distribution: Array[Response[T]])
//  extends AbstractGrammarAnswer[T](cost, question, distribution)
//case class NoGrammarAnswer[T](override val question: GrammarQuestion)
//  extends AbstractGrammarAnswer[T](0, question, Array[Response[T]]())
//case class LowConfidenceGrammarAnswer[T](value: T,
//                                  override val cost: BigDecimal,
//                                  confidence: Double,
//                                  override val question: GrammarQuestion,
//                                  override val distribution: Array[Response[T]])
//  extends AbstractGrammarAnswer[T](cost, question, distribution)
//case class OverBudgetGrammarAnswer[T](need: BigDecimal, have: BigDecimal, override val question: GrammarQuestion)
//  extends AbstractGrammarAnswer[T](need, question, Array())