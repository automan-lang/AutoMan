package edu.umass.cs.automan.adapters.mturk.mock

import java.lang
import java.lang.{Boolean, Double}

import com.amazonaws.mturk.addon.BatchItemCallback
import com.amazonaws.mturk.requester._
import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.ClientConfig

case class MockServiceState(budget: Double)

class MockRequesterService(initial_state: MockServiceState, config: ClientConfig) extends RequesterService(config) {
  var _state = initial_state

  override def approveAssignments(assignmentIds: Array[String],
                                  requesterFeedback: Array[String],
                                  defaultFeedback: String,
                                  callback: BatchItemCallback): Unit = {
    ???
  }

  override def forceExpireHIT(hitId: String): Unit = {
    ???
  }

  override def createHIT(hitTypeId: String,
                         title: String,
                         description: String,
                         keywords: String,
                         question: String,
                         reward: Double,
                         assignmentDurationInSeconds: lang.Long,
                         autoApprovalDelayInSeconds: lang.Long,
                         lifetimeInSeconds: lang.Long,
                         maxAssignments: Integer,
                         requesterAnnotation: String,
                         qualificationRequirements: Array[QualificationRequirement],
                         responseGroup: Array[String]): HIT = {
    ???
  }

  override def getHIT(hitId: String): HIT = {
    ???
  }

  override def extendHIT(hitId: String,
                         maxAssignmentsIncrement: Integer,
                         expirationIncrementInSeconds: lang.Long): Unit = {
    ???
  }

  override def rejectAssignment(assignmentId: String, requesterFeedback: String): Unit = {
    ???
  }

  override def getAllAssignmentsForHIT(hitId: String): Array[Assignment] = {
    ???
  }

  override def assignQualification(qualificationTypeId: String,
                                   workerId: String,
                                   integerValue: Integer,
                                   sendNotification: Boolean): Unit = {
    ???
  }

  override def rejectQualificationRequest(qualificationRequestId: String,
                                          reason: String): Unit = {
    ???
  }

  override def getAccountBalance: Double = {
    ???
  }

  override def revokeQualification(qualificationTypeId: String,
                                   subjectId: String,
                                   reason: String): Unit = {
    ???
  }

  override def disposeQualificationType(qualificationTypeId: String): QualificationType = {
    ???
  }

  override def getAllQualificationRequests(qualificationTypeId: String): Array[QualificationRequest] = {
    ???
  }

  override def grantQualification(qualificationRequestId: String,
                                  integerValue: Integer): Unit = {
    ???
  }

  override def createQualificationType(name: String,
                                       keywords: String,
                                       description: String): QualificationType = {
    ???
  }
}
