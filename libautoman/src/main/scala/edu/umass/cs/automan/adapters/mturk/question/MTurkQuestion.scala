package edu.umass.cs.automan.adapters.mturk.question

import edu.umass.cs.automan.core.scheduler.BackendResult
//import com.amazonaws.mturk.requester.{Assignment, QualificationRequirement}
import com.amazonaws.services.mturk.model.{QualificationRequirement, Assignment}

import xml.XML

trait MTurkQuestion {
  type R // TODO: Figure out what this is for
  type A // answer type for backend result

  protected var _description: Option[String] = None
  protected var _qualified_workers = Map[String,Set[String]]() // (QualificationTypeId -> Set[worker_id])
  protected var _formatted_content: Option[scala.xml.NodeSeq] = None
  protected var _keywords = List[String]()
  protected var _qualifications = List[QualificationRequirement]()

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
  protected[mturk] def answer(a: Assignment): BackendResult[A] = {
    new BackendResult[A](
      fromXML(XML.loadString(a.getAnswer)),
      a.getWorkerId,
      a.getAcceptTime,
      a.getSubmitTime
    )
  }
  protected[mturk] def fromXML(x: scala.xml.Node) : A
  protected[mturk] def toXML(randomize: Boolean) : scala.xml.Node
}
