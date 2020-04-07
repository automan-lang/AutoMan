package edu.umass.cs.automan.adapters.mturk.question

import java.math.BigInteger
import java.security.MessageDigest
import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.VariantOutcome
import edu.umass.cs.automan.core.grammar.Rank.Grammar
import edu.umass.cs.automan.core.grammar.{Choices, Expression, Name, OptionProduction, Production, QuestionProduction, Sequence}
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
  override var _newQ: Question = null
  override var _grammar: Grammar = null
  override var _variant: Integer = null
  //var fixed_id: UUID = this.id

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

  def merkle_hash(p: Expression): BigInt = {
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
        case q: QuestionProduction => {
          BigInt(md.digest(q.questionType.toString().getBytes)) //todo is this right?
        }
        case o: OptionProduction => {
          md5sum += merkle_hash(o.getText())
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
  override protected[mturk] def fromXML(x: Node): A = newQ.asInstanceOf[MTurkQuestion].fromXML(x).asInstanceOf[A]

  /**
    * Converts question to XML QuestionForm
    * Calls XMLBody
    *
    * @param randomize Randomize option order?
    * @return XML
    */
    // todo remove variant param

  override protected[mturk] def toXML(randomize: Boolean): Node = {
      val (body, opts) = _question.toQuestionText(variant)
      val bodyText: String = body

    question.questionType match {
      case QuestionType.EstimationQuestion => {
        newQ = new MTEstimationQuestion()
        newQ.text = bodyText
        //newQ.id = fixed_id
        //todo dear lord these casts
        newQ.asInstanceOf[MTEstimationQuestion].toXML(randomize)
      }
      case QuestionType.CheckboxQuestion => {
        newQ = new MTCheckboxQuestion()
        newQ.text = bodyText
        val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
        newQ.asInstanceOf[MTCheckboxQuestion].options = options
        newQ.asInstanceOf[MTCheckboxQuestion].toXML(randomize)
      }
      case QuestionType.CheckboxDistributionQuestion => {
        newQ = new MTCheckboxVectorQuestion()
        newQ.text = bodyText
        val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
        newQ.asInstanceOf[MTCheckboxVectorQuestion].options = options
        newQ.asInstanceOf[MTCheckboxVectorQuestion].toXML(randomize)
      }
      case QuestionType.FreeTextQuestion => {
        newQ = new MTFreeTextQuestion()
        newQ.text = bodyText
        newQ.asInstanceOf[MTFreeTextQuestion].toXML(randomize)
      }
      case QuestionType.FreeTextDistributionQuestion => {
        newQ = new MTFreeTextQuestion()
        newQ.text = bodyText
        newQ.asInstanceOf[MTFreeTextVectorQuestion].toXML(randomize)
      }
      case QuestionType.RadioButtonQuestion => {
        newQ = new MTRadioButtonQuestion()
        newQ.text = bodyText
        val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
        newQ.asInstanceOf[MTRadioButtonQuestion].options = options
        newQ.asInstanceOf[MTRadioButtonQuestion].toXML(randomize)
      }
      case QuestionType.RadioButtonDistributionQuestion => {
        newQ = new MTRadioButtonVectorQuestion()
        newQ.text = bodyText
        val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
        newQ.asInstanceOf[MTRadioButtonVectorQuestion].options = options
        newQ.asInstanceOf[MTRadioButtonVectorQuestion].toXML(randomize)
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
override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
  val (body, opts) = _question.toQuestionText(this.variant)
  val bodyText: String = body

  question.questionType match {
    case QuestionType.EstimationQuestion => {
      newQ = new MTEstimationQuestion()
      newQ.text = bodyText
      newQ.asInstanceOf[MTEstimationQuestion].toSurveyXML(randomize)
    }
    case QuestionType.CheckboxQuestion => {
      newQ = new MTCheckboxQuestion()
      newQ.text = bodyText
      val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
      newQ.asInstanceOf[MTCheckboxQuestion].options = options
      newQ.asInstanceOf[MTCheckboxQuestion].toSurveyXML(randomize)
    }
    case QuestionType.CheckboxDistributionQuestion => {
      newQ = new MTCheckboxVectorQuestion()
      newQ.text = bodyText
      val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
      newQ.asInstanceOf[MTCheckboxVectorQuestion].options = options
      newQ.asInstanceOf[MTCheckboxVectorQuestion].toSurveyXML(randomize)
    }
    case QuestionType.FreeTextQuestion => {
      newQ = new MTFreeTextQuestion()
      newQ.text = bodyText
      newQ.asInstanceOf[MTFreeTextQuestion].toSurveyXML(randomize)
    }
    case QuestionType.RadioButtonQuestion => {
      newQ = new MTRadioButtonQuestion()
      newQ.text = bodyText
      val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
      newQ.asInstanceOf[MTRadioButtonQuestion].options = options
      newQ.asInstanceOf[MTRadioButtonQuestion].toSurveyXML(randomize)
    }
    case QuestionType.RadioButtonDistributionQuestion => {
      newQ = new MTRadioButtonVectorQuestion()
      newQ.text = bodyText
      val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
      newQ.asInstanceOf[MTRadioButtonVectorQuestion].options = options
      newQ.asInstanceOf[MTRadioButtonVectorQuestion].toSurveyXML(randomize)
    }
  }
}

  //override type QuestionOptionType = this.type

  override def randomized_options: List[QuestionOptionType] = ???
}
