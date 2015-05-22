package edu.umass.cs.automan.adapters.MTurk.question

import java.util.UUID
import edu.umass.cs.automan.adapters.MTurk.mock.MockResponse
import edu.umass.cs.automan.core.scheduler.BackendResult
import com.amazonaws.mturk.requester.{Assignment, QualificationRequirement}
import xml.XML

trait MTurkQuestion {
  type R
  type A

  protected var _description: Option[String] = None
  protected var _qualified_workers = Map[String,Set[String]]() // (QualificationTypeId -> Set[worker_id])
  protected var _formatted_content: Option[scala.xml.NodeSeq] = None
  protected var _keywords = List[String]()
  protected var _mock_answers = List[A]()
  protected var _qualifications = List[QualificationRequirement]()
  protected var _group_id: Option[String] = None

  // public API
  def description_=(d: String) { _description = Some(d) }
  def description: String
  def formatted_content: scala.xml.NodeSeq = _formatted_content match {
    case Some(x) => x
    case None => scala.xml.NodeSeq.Empty
  }
  def formatted_content_=(x: scala.xml.NodeSeq) { _formatted_content = Some(x) }
  def group_id_=(id: String) { _group_id = Some(id) }
  def group_id: String
  def keywords_=(ks: List[String]) { _keywords = ks }
  def keywords: List[String] = _keywords
  def mock_answers_=(answers: List[A]) { _mock_answers = answers }
  def mock_answers: List[A] = _mock_answers
  def qualifications_=(qs: List[QualificationRequirement]) { _qualifications = qs }
  def qualifications: List[QualificationRequirement] = _qualifications

  // private API
  protected[MTurk] def answer(a: Assignment): BackendResult[A] = {
    new BackendResult[A](
      fromXML(XML.loadString(a.getAnswer)),
      a.getWorkerId,
      a.getAcceptTime.getTime,
      a.getSubmitTime.getTime
    )
  }
  protected[MTurk] def toMockResponse(question_id: UUID, a: A) : MockResponse
  protected[MTurk] def fromXML(x: scala.xml.Node) : A
  protected[MTurk] def toXML(randomize: Boolean) : scala.xml.Node
}
