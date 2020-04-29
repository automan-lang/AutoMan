package edu.umass.cs.automan.core.answer

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}

import au.com.bytecode.opencsv.CSVWriter
import edu.umass.cs.automan.core.question._
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

  /**
    * Generates a CSV file with the answer distribution
    */
  override def toString: String = {
    val outputFile = new BufferedWriter(new FileWriter("./output.csv"))
    val csvWriter = new CSVWriter(outputFile)
    val csvFields = Array("question", "answer", "count")

    // construct distribution map
    // aggregates at survey level
    var ansMap = Map[(String, String), Int]() // a map from the question ID and answers to the number of selections
    for(v <- values) {
      val qID = question.id.toString
      val response = v(question.id.toString)
      val responseString = question.prettyPrintAnswer(response.asInstanceOf[SurveyAnswers.this.question.A])

      var numAlready = 0
      if(ansMap contains ((qID, responseString))) {
        numAlready = ansMap((qID, responseString))
      }
      ansMap += ((qID, responseString) -> (numAlready + 1))
    }

    // add fields to csv
    var listOfRecords = new ListBuffer[Array[String]]()
    listOfRecords += Array(question.id.toString)
    listOfRecords += csvFields

    // add answer dist to csv
    for(a <- ansMap) {
      var toAdd = Array[String]()

      a match {
        case (strs, i) => {
          strs match {
            case (q, ans) => {
              toAdd = toAdd :+ q
              toAdd = toAdd :+ ans
              toAdd = toAdd :+ i.toString
            }
            case _ => throw new Error("parsing error")
          }
        }
        case _ => throw new Error("parsing error")
      }
      listOfRecords += toAdd
    }

    // write everything
    //val recordList: java.util.List[Array[String]] = listOfRecords
    csvWriter.writeAll(listOfRecords)
    outputFile.close()
    listOfRecords.toString()
  }

//  override def toString: String = {
//    val toRet: StringBuilder = new StringBuilder()
//    val pw = new PrintWriter(new File("answers.txt"))
//    for(v <- values) { // map from String (question ID) to Response
//      val s = v(question.id.toString)
//      val toPrint: String = question.prettyPrintAnswer(s.asInstanceOf[SurveyAnswers.this.question.A])
//      pw.write(toPrint + ",\n")
//      println(s"adding ${toPrint} to file")
//      toRet.append(toPrint)
//    }
//    pw.close()
//    toRet.toString()
//  }
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