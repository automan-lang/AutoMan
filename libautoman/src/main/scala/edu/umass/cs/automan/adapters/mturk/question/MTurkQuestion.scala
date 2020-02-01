package edu.umass.cs.automan.adapters.mturk.question

import edu.umass.cs.automan.core.scheduler.BackendResult

import scala.xml.Node
//import com.amazonaws.mturk.requester.{Assignment, QualificationRequirement}
import com.amazonaws.services.mturk.model.{QualificationRequirement, Assignment}

import xml.XML

// Adding MTurk stuff
trait MTurkQuestion {
  type A // answer type for backend result

  protected var _description: Option[String] = None // description of task shown to worker
  protected var _qualified_workers = Map[String,Set[String]]() // (QualificationTypeId -> Set[worker_id])
  protected var _formatted_content: Option[scala.xml.NodeSeq] = None // TODO: what is this, maybe delete
  protected var _keywords = List[String]() // keywords for searching in MTurk
  protected var _qualifications = List[QualificationRequirement]() // qualification list for workers

  // public API
  def description_=(d: String) { _description = Some(d) }
  def description: String
  def formatted_content: scala.xml.NodeSeq = _formatted_content match {
    case Some(x) => x
    case None => scala.xml.NodeSeq.Empty
  }
  def formatted_content_=(x: scala.xml.NodeSeq) { _formatted_content = Some(x) }
  def group_id: String
  def keywords_=(ks: List[String]) { _keywords = ks }
  def keywords: List[String] = _keywords
  def qualifications_=(qs: List[QualificationRequirement]) { _qualifications = qs }
  def qualifications: List[QualificationRequirement] = _qualifications

  // private API
  /**
    * Converts XML answer into answer type expected by question
    * @param a MTurk SDK Assignment
    * @return Answer value
    */
  protected[mturk] def answer(a: Assignment): BackendResult[A] = {
    new BackendResult[A](
      fromXML(XML.loadString(a.getAnswer)),
      a.getWorkerId,
      a.getAcceptTime,
      a.getSubmitTime
    )
  }

  /**
    * Parses answer from XML
    * @param x the XML
    * @return Answer value
    */
  protected[mturk] def fromXML(x: scala.xml.Node) : A

  /**
    * Converts question to XML QuestionForm
    * Calls XMLBody
    * @param randomize Randomize option order?
    * @return XML
    */
  protected[mturk] def toXML(randomize: Boolean, variant: Int) : scala.xml.Node

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    * @param randomize Randomize option order?
    * @return XML
    */
  protected[mturk] def XMLBody(randomize: Boolean) : Seq[scala.xml.Node]

  protected[mturk] def toSurveyXML(randomize: Boolean) : scala.xml.Node
}
