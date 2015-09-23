package edu.umass.cs.automan.adapters.mturk.logging.tables

import java.util.{Calendar, UUID}
import com.amazonaws.mturk.requester.AssignmentStatus
import scala.slick.driver.H2Driver.simple._

object DBAssignment {
  def statusMapper = MappedColumnType.base[AssignmentStatus, Int](
  {
    case AssignmentStatus.Approved => 0
    case AssignmentStatus.Rejected => 1
    case AssignmentStatus.Submitted => 2
  },
  {
    case 0 => AssignmentStatus.Approved
    case 1 => AssignmentStatus.Rejected
    case 2 => AssignmentStatus.Submitted
  }
  )
  def calendarMapper = MappedColumnType.base[java.util.Calendar, java.sql.Timestamp](
    d => new java.sql.Timestamp(d.getTimeInMillis),
    d => {
      val cal = Calendar.getInstance()
      cal.setTimeInMillis(d.getTime)
      cal
    }
  )

  implicit val javaUtilDateMapper =
    MappedColumnType.base[java.util.Date, java.sql.Timestamp] (
      d => new java.sql.Timestamp(d.getTime),
      d => new java.util.Date(d.getTime))
}

class DBAssignment(tag: Tag) extends Table[(String, String, String, AssignmentStatus, Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], Option[Calendar], String, Option[String], UUID)](tag, "DBAssignment") {
  implicit val statusMapper = DBAssignment.statusMapper
  implicit val calendarMapper = DBAssignment.calendarMapper

  def assignmentId = column[String]("assignmentId", O.PrimaryKey)
  def workerId = column[String]("workerId")
  def HITId = column[String]("HITId")
  def assignmentStatus = column[AssignmentStatus]("assignmentStatus")
  def autoApprovalTime = column[Option[Calendar]]("autoApprovalTime")
  def acceptTime = column[Option[Calendar]]("acceptTime")
  def submitTime = column[Option[Calendar]]("submitTime")
  def approvalTime = column[Option[Calendar]]("approvalTime")
  def rejectionTime = column[Option[Calendar]]("rejectionTime")
  def deadline = column[Option[Calendar]]("deadline")
  def answer = column[String]("answer")
  def requesterFeedback = column[Option[String]]("requesterFeedback")
  def taskId = column[UUID]("taskId")
  override def * = (assignmentId, workerId, HITId, assignmentStatus, autoApprovalTime, acceptTime, submitTime, approvalTime, rejectionTime, deadline, answer, requesterFeedback, taskId)
}