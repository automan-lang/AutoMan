package edu.umass.cs.automan.adapters.MTurk.actor

import akka.actor.Actor
import edu.umass.cs.automan.adapters.MTurk.question._
import edu.umass.cs.automan.core.scheduler.{Thunk, SchedulerState}
import edu.umass.cs.automan.core.answer.{Answer, FreeTextAnswer, CheckboxAnswer, RadioButtonAnswer}
import com.amazonaws.mturk.service.axis.RequesterService
import scala.collection.mutable
import edu.umass.cs.automan.adapters.MTurk.{MTurkAdapter, AutomanHIT}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.adapters.MTurk.actor.messages._
import edu.umass.cs.automan.adapters.MTurk.question.HITState.HITState
import edu.umass.cs.automan.adapters.MTurk.question.HITState
import java.util.UUID
import com.amazonaws.mturk.requester.{Comparator, QualificationType, QualificationRequirement}
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

class MTurkActor(backend: RequesterService, sleep_ms: Int) extends Actor {
  val answers = mutable.Map[Thunk, Answer]()
  val hits = mutable.Map[MTurkQuestion, List[AutomanHIT]]()
  val hit_state = mutable.Map[AutomanHIT, HITState]()
  val assignments = mutable.Map[Thunk, String]()
  val blacklisted_workers = mutable.Map[MTurkQuestion, List[String]]()

  def receive = {
    // ACCEPT
    case AcceptRequest(t) =>
      // send approval request
      backend.approveAssignment(assignments(t), "Thanks!")

      // update paid status:
      //   1. on object
      //   2. in database
      answers(t) match {
        case rba: RadioButtonAnswer => if (!rba.paid) {
          rba.memo_handle.setPaidStatus(true)
          rba.memo_handle.save()
          rba.paid = true
        }
        case cba: CheckboxAnswer => if (!cba.paid) {
          cba.memo_handle.setPaidStatus(true)
          cba.memo_handle.save()
          cba.paid = true
        }
        case fta: FreeTextAnswer => if (!fta.paid) {
          fta.memo_handle.setPaidStatus(true)
          fta.memo_handle.save()
          fta.paid = true
        }
      }
      // TODO: check that all assignments are paid and update status?

      // rate-limit
      sleep

      // acknowledge approval is done
      sender ! AcceptResponse()

    // CANCEL
    case CancelRequest(t) => {
      val mtquestion = t.question match { case mtq: MTurkQuestion => mtq; case _ => throw new Exception("Impossible.") }
      hits(mtquestion).filter{hit_state(_) == HITState.RUNNING}.foreach { hit =>
        backend.forceExpireHIT(hit.hit.getHITId)
        hit_state(hit) = HITState.RESOLVED
      }
      // TODO: thunk state should be managed by scheduler
//          t.state = SchedulerState.REJECTED
      // rate-limit
      sleep

      // acknowledge cancellation is done
      sender ! CancelResponse()
    }

    // POST
    case PostRequest(ts: List[Thunk], dual: Boolean, exclude_worker_ids: List[String]) => {
      val question = MTurkAdapter.question_for_thunks(ts)
      val mtquestion = question match { case mtq: MTurkQuestion => mtq; case _ => throw new Exception("Impossible.") }
      if (!blacklisted_workers.contains(mtquestion)) {
        blacklisted_workers += (mtquestion -> mtquestion.blacklisted_workers)
      }
      val qualify_early = if (blacklisted_workers(mtquestion).size > 0) true else false
      val quals = get_qualifications(mtquestion, ts.head.question.text, qualify_early, question.id)

      // TODO: get HITTypeId if one exists
      val htid = ""

      // Build HIT and add it to post queue
      mtquestion match {
        case rbq: MTRadioButtonQuestion => {
          rbq.build_hit(ts, is_dual = false, quals, htid).post(backend)
        }
        case cbq: MTCheckboxQuestion => {
          cbq.build_hit(ts, dual, quals, htid).post(backend)
        }
        case ftq: MTFreeTextQuestion => {
          ftq.build_hit(ts, is_dual = false, quals, htid).post(backend)
        }
        case _ => throw new Exception("Question type not yet supported.  Question class is " + mtquestion.getClass)
      }
      // TODO: thunk state should be managed by scheduler
//      ts.foreach { _.state = SchedulerState.RUNNING }
      // TODO: _retrieval_queue ++= ts

      // rate-limit
      sleep

      // acknowledge post is done
      sender ! PostResponse()
    }
  }

  // private methods
  private def get_qualifications(q: MTurkQuestion, title: String, qualify_early: Boolean, question_id: UUID) : List[QualificationRequirement] = {
    // The first qualification always needs to be the special
    // "dequalification" type so that we may grant it as soon as
    // a worker completes some work.
    if (q.firstrun) {
      Utilities.DebugLog("This is the task's first run; creating dequalification.",LogLevel.INFO,LogType.ADAPTER,question_id)
      val qual : QualificationType = backend.createQualificationType("AutoMan " + UUID.randomUUID(),
        "automan", "AutoMan automatically generated Qualification (title: " + title + ")")
      val deq = new QualificationRequirement(qual.getQualificationTypeId, Comparator.NotEqualTo, 1, null, false)
      q.dequalification = deq
      q.firstrun = false
      // we need early qualifications; add anyway
      if (qualify_early) {
        q.qualifications = deq :: q.qualifications
      }
    } else if (!q.qualifications.contains(q.dequalification)) {
      // add the dequalification to the list of quals if this
      // isn't a first run and it isn't already there
      q.qualifications = q.dequalification :: q.qualifications
    }
    q.qualifications
  }
//  private def get_hittypeid(service: RequesterService, cost: BigDecimal, quals: List[QualificationRequirement]) : String = {
//    // find the HIT Type if we don't have one already, otherwise create a new one
//    // new costs are reason enough for a new hit type
//    hit_type_id match {
//      // the user may have defined one
//      case Some(htid) => htid
//      // or they may not care at all
//      // we just need to ask AMT for one
//      case None => {
//        val htid = service.registerHITType(autoApprovalDelayInSeconds, assignmentDurationInSeconds, cost.toDouble, title, keytext, description, quals.toArray)
//        hit_type_id = Some(htid)
//        htid
//      }
//    }
//  }
  private def sleep = Thread.sleep(sleep_ms)
}