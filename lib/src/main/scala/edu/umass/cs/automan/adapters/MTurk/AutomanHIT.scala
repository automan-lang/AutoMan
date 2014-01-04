package edu.umass.cs.automan.adapters.MTurk

import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.requester._
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities, retry}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}
import question.{HITState, MTRadioButtonQuestion}
import HITState._
import java.util.UUID
import edu.umass.cs.automan.adapters.MTurk.question.HITState
import scala.Some

object AutomanHIT {
  def apply(init: AutomanHIT => Unit) : AutomanHIT = {
    val a = new AutomanHIT
    init(a)
    a
  }
}

class AutomanHIT {
  var assignmentDurationInSeconds: Long = _
  var assignments = List[Assignment]()
  var autoApprovalDelayInSeconds: Long = 2592000 // AutoMan approves Assns, not the user or MTurk, so mthis is the MTurk MAX
  var cost: BigDecimal = 0.01
  var description: String = null
  var hit: HIT = _
  var hit_type_id: Option[String] = None
  var id: UUID = _
  var keywords = List[String]("automan")
  var lifetimeInSeconds: Long = _
  var maxAssignments: Int = 1
  var qualifications = List[QualificationRequirement]()
  var question_xml: xml.Node = _
  var requesterAnnotation: String = "automan" // takes an arbitrary string; currently undefined
  var responseGroup = List[String]()
  var state: HITState = HITState.READY
  var thunk_assignment_map = Map[Thunk,Assignment]()
  var title: String = null

  def cancel(service: RequesterService) {
    if (state != HITState.RESOLVED ) {
      service.forceExpireHIT(hit.getHITId)
    }
  }
  // returns a hit type id
  def post(service: RequesterService) : String = {
    val htid = hittype(service, cost, qualifications)
    Utilities.DebugLog("Posting HIT with type: " + htid + " and qualifications: " +
                       qualifications.map(_.getQualificationTypeId).foldLeft("")((acc,q) => acc + ", " + q) +
                       " and " + maxAssignments + " assignments.",LogLevel.INFO,LogType.ADAPTER,id)
    hit = service.createHIT(null,
                            title,
                            description,
                            keytext,
                            question_xml.toString(),
                            cost.toDouble,
                            assignmentDurationInSeconds,
                            autoApprovalDelayInSeconds,
                            lifetimeInSeconds,
                            maxAssignments,
                            requesterAnnotation,
                            qualifications.toArray,
                            responseGroup.toArray)
    state = HITState.RUNNING
    htid
  }
  def retrieve(service: RequesterService) : List[Assignment] = {
    // get new assignments
    var assns = List[Assignment]()
    Utilities.DebugLog("Getting assignments...",LogLevel.INFO,LogType.ADAPTER,id)
    assns = retry(5) {
      service.getAllAssignmentsForHIT(
        hit.getHITId, Array(AssignmentStatus.Submitted)
      ).toList
    }
    
    // we only care about NEW Assignments
    val new_assns = assns.filter{ a => !assignments.contains(a) }

    // add to our list of Assignments
    assignments = new_assns ::: assignments

    new_assns
  }
  def hittype(service: RequesterService, cost: BigDecimal, quals: List[QualificationRequirement]) : String = {
    // find the HIT Type if we have one already, otherwise create a new one
    // new costs are reason enough for a new hit type
    hit_type_id match {
      // the user may have defined one
      case Some(htid) => htid
      // or they may not care at all
      // we just need to ask AMT for one
      case None => {
        val htid = service.registerHITType(autoApprovalDelayInSeconds, assignmentDurationInSeconds, cost.toDouble, title, keytext, description, quals.toArray)
        hit_type_id = Some(htid)
        htid
      }
    }
  }
  def keytext : String = keywords.foldLeft(""){ (str, keyword) => {str + ", " + keyword } }

  override def toString = {
    var str = ""
    str += "\ntitle: " + title
    str += "\ndescription: " + description
    str += "\nkeywords: " + keywords
    str += "\nquestion_xml: " + question_xml
    str += "\nassignmentDurationInSeconds: " + assignmentDurationInSeconds
    str += "\nautoApprovalDelayInSeconds: " + autoApprovalDelayInSeconds
    str += "\nlifetimeInSeconds: " + lifetimeInSeconds
    str += "\nmaxAssignments: " + maxAssignments
    str += "\nrequesterAnnotation: " + requesterAnnotation
    str += "\nresponseGroup: " + responseGroup
    str += "\n"
    str
  }
}
