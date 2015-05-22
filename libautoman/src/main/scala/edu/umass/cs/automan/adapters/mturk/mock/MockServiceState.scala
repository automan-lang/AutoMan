package edu.umass.cs.automan.adapters.MTurk.mock

import java.io.Serializable
import java.util.UUID
import com.amazonaws.mturk.requester.HIT
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.util.Utilities

case class MockServiceState(budget: java.math.BigDecimal,
                            questions_by_question_id: Map[UUID,Question],
                            hit_type_by_hit_type_id: Map[String,MockHITType],
                            hits_by_question_id: Map[UUID, List[HIT]],
                            answers_by_assignment_id: Map[UUID,MockResponse],
                            assignment_status_by_assignment_id: Map[UUID,(AssignmentStatus.Value,Option[String])],
                            assignment_ids_by_question_id: Map[UUID, List[UUID]]) {
  def addHIT(question_id: UUID, hit: HIT) : MockServiceState = {
    val state2 = reserveAssignments(question_id, hit.getHITId, hit.getMaxAssignments)

    // update hit list
    val hitlist = hit :: (
      if (state2.hits_by_question_id.contains(question_id)) {
        state2.hits_by_question_id(question_id)
      } else {
        List.empty
      })

    // return a new MockServiceState
    MockServiceState(
      state2.budget,
      state2.questions_by_question_id,
      state2.hit_type_by_hit_type_id,
      state2.hits_by_question_id + (question_id -> hitlist),
      state2.answers_by_assignment_id,
      state2.assignment_status_by_assignment_id,
      state2.assignment_ids_by_question_id
    )
  }
  def reserveAssignments(question_id: UUID, hit_id: String, num: Int) : MockServiceState = {
    // reserve the correct number of unreserved assignments for the hit
    val reserved_ids = assignment_ids_by_question_id(question_id).flatMap { a_id =>
      if (assignment_status_by_assignment_id(a_id)._1 == AssignmentStatus.UNANSWERED) {
        Some(a_id)
      } else {
        None
      }
    }.take(num)

    assert(reserved_ids.size == num)

    // update status map
    reserved_ids.foldLeft(this) { case (state: MockServiceState, a_id: UUID) =>
        state.updateAssignmentStatus(a_id, Some(hit_id), AssignmentStatus.ANSWERED)
    }
  }
  def addHITType(hit_type: MockHITType) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id + (hit_type.id.toString -> hit_type),
      hits_by_question_id,
      answers_by_assignment_id,
      assignment_status_by_assignment_id,
      assignment_ids_by_question_id
    )
  }
  def budgetDelta(delta: java.math.BigDecimal) : MockServiceState = {
    assert (delta.compareTo(budget) != 1)
    MockServiceState(
      budget.add(delta),
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      assignment_status_by_assignment_id,
      assignment_ids_by_question_id
    )
  }
  private def cloneHIT(hit: HIT) : HIT = {
    val cloned_hit = new HIT()
    cloned_hit.setAssignmentDurationInSeconds(hit.getAssignmentDurationInSeconds)
    cloned_hit.setAutoApprovalDelayInSeconds(hit.getAutoApprovalDelayInSeconds)
    cloned_hit.setCreationTime(hit.getCreationTime)
    cloned_hit.setDescription(hit.getDescription)
    cloned_hit.setExpiration(hit.getExpiration)
    cloned_hit.setHITGroupId(hit.getHITGroupId)
    cloned_hit.setHITId(hit.getHITId)
    cloned_hit.setHITLayoutId(hit.getHITLayoutId)
    cloned_hit.setHITReviewStatus(hit.getHITReviewStatus)
    cloned_hit.setHITStatus(hit.getHITStatus)
    cloned_hit.setHITTypeId(hit.getHITTypeId)
    cloned_hit.setKeywords(hit.getKeywords)
    cloned_hit.setMaxAssignments(hit.getMaxAssignments)
    cloned_hit.setNumberOfAssignmentsAvailable(hit.getNumberOfAssignmentsAvailable)
    cloned_hit.setNumberOfAssignmentsCompleted(hit.getNumberOfAssignmentsCompleted)
    cloned_hit.setNumberOfAssignmentsPending(hit.getNumberOfAssignmentsPending)
    cloned_hit.setQualificationRequirement(hit.getQualificationRequirement)
    cloned_hit.setQuestion(hit.getQuestion)
    cloned_hit.setRequest(hit.getRequest)
    cloned_hit.setRequesterAnnotation(hit.getRequesterAnnotation)
    cloned_hit.setReward(hit.getReward)
    cloned_hit.setTitle(hit.getTitle)
    cloned_hit
  }
  def extendHIT(hitId: String, deltaSec: Int, deltaAssignments: Int) = {
    assert(hits_by_question_id.size >= 1)

    // get HIT
    val hit = getHITforHITId(hitId)
    val question_id = UUID.fromString(hit.getRequesterAnnotation)

    // clone
    val cloned_hit = cloneHIT(hit)

    // update selected fields
    cloned_hit.setExpiration(Utilities.calInSeconds(cloned_hit.getExpiration, deltaSec))
    cloned_hit.setMaxAssignments(cloned_hit.getMaxAssignments + deltaAssignments)

    // reserve assignments
    val state2 = reserveAssignments(question_id, hitId, deltaAssignments)

    // update hit list
    val hitlist = cloned_hit :: state2.hits_by_question_id(question_id).filter(_.getHITId != hitId)

    MockServiceState(
      state2.budget,
      state2.questions_by_question_id,
      state2.hit_type_by_hit_type_id,
      state2.hits_by_question_id + (question_id -> hitlist),
      state2.answers_by_assignment_id,
      state2.assignment_status_by_assignment_id,
      state2.assignment_ids_by_question_id
    )
  }
  def getHITforHITId(hitId: String) : HIT = {
    assert(hits_by_question_id.size >= 1)
    hits_by_question_id.flatMap(_._2).filter(_.getHITId == hitId).head
  }
  def addQuestion(question: Question) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id + (question.id -> question),
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      assignment_status_by_assignment_id,
      assignment_ids_by_question_id
    )
  }
  def addAssignments(question_id: UUID, assignments: Map[UUID,MockResponse]) : MockServiceState = {
    val answers = answers_by_assignment_id ++ assignments
    val status = assignment_status_by_assignment_id ++
                 assignments.map { case (id,a) => id -> (AssignmentStatus.UNANSWERED, None) }
    val a_by_q = assignment_ids_by_question_id + (question_id -> assignments.map(_._1).toList)
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers,
      status,
      a_by_q
    )
  }
  def updateAssignmentStatusMap(am: Map[UUID,(AssignmentStatus.Value,Option[String])]) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      am,
      assignment_ids_by_question_id
    )
  }
  def updateAssignmentStatus(assignmentId: UUID, new_status: AssignmentStatus.Value) : MockServiceState = {
    updateAssignmentStatus(assignmentId, None, new_status)
  }
  def updateAssignmentStatus(assignmentId: UUID, hit_id_opt: Option[String], new_status: AssignmentStatus.Value) : MockServiceState = {
    MockServiceState(
      budget,
      questions_by_question_id,
      hit_type_by_hit_type_id,
      hits_by_question_id,
      answers_by_assignment_id,
      changeAssignmentStatus(assignmentId, hit_id_opt, new_status, assignment_status_by_assignment_id),
      assignment_ids_by_question_id
    )
  }
  private def changeAssignmentStatus(assignmentId: UUID, hit_id_opt: Option[String], new_status: AssignmentStatus.Value, assn_map: Map[UUID,(AssignmentStatus.Value,Option[String])])
  : Map[UUID,(AssignmentStatus.Value,Option[String])] = {
    val current_status = assn_map(assignmentId)._1

    // Ensure that only valid state transitions are allowed
    new_status match {
      case AssignmentStatus.APPROVED => assert(current_status == AssignmentStatus.ANSWERED)
      case AssignmentStatus.REJECTED => assert(current_status == AssignmentStatus.ANSWERED)
      case AssignmentStatus.ANSWERED => assert(current_status == AssignmentStatus.UNANSWERED)
      case _ => assert(false)
    }

    val hio = hit_id_opt match {
      case None => assn_map(assignmentId)._2
      case Some(h) => Some(h)
    }
    assn_map + (assignmentId -> (new_status, hio))
  }
}
