package edu.umass.cs.automan.adapters.mturk.question

import java.security.MessageDigest
import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.VariantOutcome
import edu.umass.cs.automan.core.grammar.{Grammar, QuestionProduction}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.{EstimationQuestion, Question, VariantQuestion}
import edu.umass.cs.automan.core.util.Utilities
import org.apache.commons.codec.binary.Hex

import scala.collection.mutable
import scala.xml.Node

class MTVariantQuestion extends VariantQuestion with MTurkQuestion {
  //override type QuestionOptionType = this.type
  override var _question: QuestionProduction = null
  override var newQ: Question = null
  override var _grammar: Grammar = null

//  override type A = this.type
//  override type AA = this.type
//  override type O = this.type
//  override type AP = this.type
//  override type PP = this.type
//  override type TP = this.type

  //override def randomized_options: List[QuestionOptionType] = ???

  //override type A = question

//  override type AA = this.type
//  override type O = this.type
//  override type AP = this.type
//  override type PP = this.type
//  override type TP = this.type

  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    //val hSet = new mutable.HashSet[String]()
    val r = new scala.util.Random()
    val numPoss = grammar.count(0, new mutable.HashSet[String]())
    //val numPoss = _question.count(grammar.count(0, hSet), new mutable.HashSet[String]())
    new String(Hex.encodeHex(md.digest(toXML(randomize = false, r.nextInt(numPoss)).toString().getBytes)))
  }

  //override type A = VariantQuestion#A

  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }
  //override def randomized_options: List[QuestionOptionType] = Utilities.randomPermute(options)

  /**
    * Parses answer from XML
    *
    * @param x the XML
    * @return Answer value
    */
  override protected[mturk] def fromXML(x: Node): A = ???

  /**
    * Converts question to XML QuestionForm
    * Calls XMLBody
    *
    * @param randomize Randomize option order?
    * @return XML
    */
override protected[mturk] def toXML(randomize: Boolean, variant: Int): Node = {
  val bodyText: String = _question.toQuestionText(variant)._1 // todo hardcode magic numbers?
  val options: List[String] = _question.toQuestionText(variant)._2
  question.questionType match {
    case QuestionType.EstimationQuestion => {
      //text = bodyText
      newQ = new MTEstimationQuestion()
      newQ.text = bodyText
      //val newQ: MTEstimationQuestion = this.asInstanceOf[MTEstimationQuestion].cloneWithConfidence(_confidence).asInstanceOf[MTEstimationQuestion]
      //todo dear lord these casts
      newQ.asInstanceOf[MTEstimationQuestion].toXML(randomize, variant)
    }
  }
}

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
override protected[mturk] def XMLBody(randomize: Boolean): Seq[Node] = ???
override protected[mturk] def toSurveyXML(randomize: Boolean): Node = ???

  //override type QuestionOptionType = this.type

  override def randomized_options: List[QuestionOptionType] = ???
}
