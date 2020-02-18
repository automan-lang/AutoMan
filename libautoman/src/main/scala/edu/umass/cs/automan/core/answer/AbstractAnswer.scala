package edu.umass.cs.automan.core.answer

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

//import au.com.bytecode.opencsv.CSVWriter
import edu.umass.cs.automan.core.question._

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
case class SurveyAnswers(values: Seq[Map[String,Question#A]], // final dist (no worker ids) // todo values: Set[(String, Question#A)],?
                         override val cost: BigDecimal,
                         override val question: Survey,
                         override val distribution: Array[Response[Set[(String,Question#A)]]])
  extends AbstractSurveyAnswer(cost, question, distribution) {
  override def toString: String = {
    val toRet: StringBuilder = new StringBuilder()

    //println("Dan is sort of right")
    //val s = values.head("")
    //val outputFile = new BufferedWriter(new FileWriter("answers.text"))
    val pw = new PrintWriter(new File("answers.txt"))
    //val csvWriter = new CSVWriter(outputFile)
    //var toRecord: ListBuffer[Array[String]] = new ListBuffer[Array[String]]()
    //var toRecord: ListBuffer[String] = new ListBuffer[String]()

//    for(v <- distribution){
//      v match {
//        case r: Response[Set[(String,Question#A)]] => {
//          val s: Map[String, Question#A] = r.value.flatMap(m => {
//            m.map {
//              case (str, q) => {
//                str -> q
//              }
//            }
//          })
//          val s = v.value.get(question.name)
//          val toPrint: String = question.prettyPrintAnswer(s.asInstanceOf[SurveyAnswers.this.question.A])
//          pw.write(toPrint + ",")
//          println(s"adding ${toPrint} to file")
//          toRet.append(toPrint)
//        }
//      }
    for(v <- values) {
      val s = v(question.id.toString)
      val toPrint: String = question.prettyPrintAnswer(s.asInstanceOf[SurveyAnswers.this.question.A])
      pw.write(toPrint + ",\n")
      println(s"adding ${toPrint} to file")
      //csvWriter.write(toPrint)
      //toRecord += Array(toPrint)
      //toRecord += toPrint
      toRet.append(toPrint)
    }
    pw.close()
    //csvWriter.writeAll(toRecord.toList)

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
    toRet.toString()
    //question.prettyPrintAnswer(s.asInstanceOf[SurveyAnswers.this.question.A]) // survey A = Set[(String,Question#A)] values.asInstanceOf[this.question.A]
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