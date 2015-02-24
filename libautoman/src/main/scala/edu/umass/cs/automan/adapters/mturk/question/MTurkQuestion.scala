package edu.umass.cs.automan.adapters.mturk.question

import java.text.SimpleDateFormat
import java.util.Date

import edu.umass.cs.automan.adapters.mturk.AutomanHIT
import edu.umass.cs.automan.core.scheduler.{BackendResult, Thunk}
import com.amazonaws.mturk.requester.{Assignment, QualificationRequirement}

trait MTurkQuestion {
  type A
  type Group <: MTQuestionGroup

  protected[automan] var disqualification : QualificationRequirement = _
  protected[automan] var firstrun: Boolean = true
  protected[automan] var hits = List[AutomanHIT]()
  protected[automan] var hit_thunk_map = Map[AutomanHIT,List[Thunk[_]]]()
  protected[automan] var thunk_assnid_map = Map[Thunk[_],String]() // maps thunks to assignment ids
  protected var _description: String = ""
  protected var _qualified_workers = Map[String,Set[String]]() // (QualificationTypeId -> Set[worker_id])
  protected var _formatted_content: Option[scala.xml.NodeSeq] = None
  protected var _hit_type_id: Option[String] = None
  protected var _keywords = List[String]()
  protected var _qualifications = List[QualificationRequirement]()
  protected val _mt_date_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  protected var _question_group: Group

  def answer(a: Assignment): BackendResult[A]
  def build_hit(ts: List[Thunk[_]]) : AutomanHIT
  def description_=(d: String) { _description = d }
  def description: String = _description
  def formatted_content: scala.xml.NodeSeq = _formatted_content match {
    case Some(x) => x
    case None => scala.xml.NodeSeq.Empty
  }
  def formatted_content_=(x: scala.xml.NodeSeq) { _formatted_content = Some(x) }
  def group_=(id: String)
  def group: String = _question_group.group_id
  def hit_for_thunk(t: Thunk[_]) : AutomanHIT = {
    var the_hit: AutomanHIT = null
    hits.foreach { h =>
      if (h.thunk_assignment_map.contains(t)) {
        the_hit = h
      }
    }
    the_hit
  }
  def hit_type_id: String = _hit_type_id match { case Some(x) => x; case None => "" }
  def hit_type_id_=(s: String) { _hit_type_id = Some(s) }
  def keywords_=(ks: List[String]) { _keywords = ks }
  def keywords: List[String] = _keywords
  protected def acceptTimeFromXML(x: scala.xml.Node) : Date = {
    _mt_date_format.parse((x \\ "AcceptTime").text)
  }
  protected def submitTimeFromXML(x: scala.xml.Node) : Date = {
    _mt_date_format.parse((x \\ "SubmitTime").text)
  }
  def qualifications_=(qs: List[QualificationRequirement]) { _qualifications = qs }
  def qualifications: List[QualificationRequirement] = _qualifications
  def qualify_worker(qualification_type_id: String, worker_id: String) {
    // initialize set if it's not in the map
    if (!_qualified_workers.contains(qualification_type_id)) {
      _qualified_workers += (qualification_type_id -> Set[String]())
    }
    // append worker_id to set and reinsert in map
    // (must be reinserted because Set is immutable)
    var worker_ids = _qualified_workers(qualification_type_id)
    worker_ids += worker_id
    _qualified_workers += (qualification_type_id -> worker_ids)
  }
  def worker_is_qualified(qualification_type_id: String, worker_id: String) : Boolean = {
    // initialize set if it's not in the map
    if (!_qualified_workers.contains(qualification_type_id)) {
      _qualified_workers += (qualification_type_id -> Set[String]())
    }
    // check for membership in worker_id list
    _qualified_workers(qualification_type_id).contains(worker_id)
  }
  def toXML(randomize: Boolean) : scala.xml.Node
}
