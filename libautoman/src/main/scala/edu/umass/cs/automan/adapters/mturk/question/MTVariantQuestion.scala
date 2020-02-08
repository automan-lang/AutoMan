package edu.umass.cs.automan.adapters.mturk.question

import java.math.BigInteger
import java.security.MessageDigest
import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.VariantOutcome
import edu.umass.cs.automan.core.grammar.{Choices, Grammar, Name, Production, QuestionProduction, Sequence}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.{EstimationQuestion, Question, VariantQuestion}
import edu.umass.cs.automan.core.util.Utilities
import org.apache.commons.codec.binary.Hex

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
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

  def memo_hash: String = {
    //val md: MessageDigest = MessageDigest.getInstance("md5")
    //val hSet = new mutable.HashSet[String]()
//    val r = new scala.util.Random()
//    val numPoss = grammar.count(0, new mutable.HashSet[String]())
    //val numPoss = _question.count(grammar.count(0, hSet), new mutable.HashSet[String]())
    //new String(Hex.encodeHex(md.digest(toXML(randomize = false, r.nextInt(numPoss)).toString().getBytes)))
    val startProd = _grammar.rules.get(_grammar.startSymbol)
    startProd match {
      case Some(prod) => {
        val toRet = new String(Hex.encodeHex(merkle_hash(prod).toString().getBytes))
        toRet
      }
      case None => {
        throw new Error("Something has gone very wrong while hashing.")
      }
    }
    //val toRet = new String(Hex.encodeHex(merkle_hash(_grammar.rules.get(_grammar.startSymbol))))
    //toRet
  }

  def merkle_hash(p: Production): BigInt = {
    val md: MessageDigest = MessageDigest.getInstance("md5")
    if(p.isLeafProd()) BigInt(md.digest(p.sample().getBytes()))
    else {
      var md5sum: BigInt = BigInt("0")
      p match {
        case c: Choices => {
          for(o <- c.getOptions()) md5sum += merkle_hash(o)
        }
        case s: Sequence => {
          for(o <- s.getList()) md5sum += merkle_hash(o)
        }
        case n: Name => {
          val res = _grammar.rules.get(n.sample())
          res match {
            case Some(r) => md5sum += merkle_hash(r)
            case None => throw new Error(s"Symbol ${res} could not be found while hashing.")
          }
        }
      }
      md5sum
    }
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
  val (body, opts) = _question.toQuestionText(variant)
  val bodyText: String = body

  question.questionType match {
    case QuestionType.EstimationQuestion => {
      newQ = new MTEstimationQuestion()
      newQ.text = bodyText
      //todo dear lord these casts
      newQ.asInstanceOf[MTEstimationQuestion].toXML(randomize, variant)
    }
    case QuestionType.CheckboxQuestion => {
      newQ = new MTCheckboxQuestion()
      newQ.text = bodyText
      val options: List[MTQuestionOption] = opts.map(new MTQuestionOption(Symbol(newQ.id.toString()), _, ""))
      newQ.asInstanceOf[MTCheckboxQuestion].options = options
      newQ.asInstanceOf[MTCheckboxQuestion].toXML(randomize, variant)
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
override protected[mturk] def XMLBody(randomize: Boolean, variant: Int): Seq[Node] = ???
override protected[mturk] def toSurveyXML(randomize: Boolean, variant: Int): Node = {
  val (body, opts) = _question.toQuestionText(variant)
  val bodyText: String = body

  question.questionType match {
    case QuestionType.EstimationQuestion => {
      newQ = new MTEstimationQuestion()
      newQ.text = bodyText
      //todo dear lord these casts
      newQ.asInstanceOf[MTEstimationQuestion].toSurveyXML(randomize, variant)
    }
    case QuestionType.CheckboxQuestion => {
      newQ = new MTCheckboxQuestion()
      newQ.text = bodyText
      val options: List[MTQuestionOption] = opts.map(new MTQuestionOption(Symbol(newQ.id.toString()), _, ""))
      newQ.asInstanceOf[MTCheckboxQuestion].options = options
      newQ.asInstanceOf[MTCheckboxQuestion].toSurveyXML(randomize, variant)
    }
  }
}

  //override type QuestionOptionType = this.type

  override def randomized_options: List[QuestionOptionType] = ???
}
