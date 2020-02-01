package edu.umass.cs.automan.adapters.mturk.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.grammar.QuestionProduction
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.VariantQuestion
import edu.umass.cs.automan.core.util.Utilities
import org.apache.commons.codec.binary.Hex

import scala.xml.Node

class MTVariantQuestion extends VariantQuestion with MTurkQuestion {
  //override type QuestionOptionType = this.type
  override var _question: QuestionProduction = null
  override type QuestionOptionType = this.type

  override type A = this.type
  override type AA = this.type
  override type O = this.type
  override type AP = this.type
  override type PP = this.type
  override type TP = this.type

  override def randomized_options: List[QuestionOptionType] = ???

  //override type A = question

//  override type AA = this.type
//  override type O = this.type
//  override type AP = this.type
//  override type PP = this.type
//  override type TP = this.type

  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
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
override protected[mturk] def toXML(randomize: Boolean): Node = ???

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
override protected[mturk] def XMLBody(randomize: Boolean): Seq[Node] = ???
override protected[mturk] def toSurveyXML(randomize: Boolean): Node = ???

}
