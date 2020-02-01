package edu.umass.cs.automan.adapters.mturk.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.grammar.QuestionProduction
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.VariantQuestion

import scala.xml.Node

class MTVariantQuestion extends VariantQuestion with MTurkQuestion {
  override var question: QuestionProduction = _
  //override type A = VariantQuestion#A

  override def description: String = ???

  override def group_id: String = ???

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
