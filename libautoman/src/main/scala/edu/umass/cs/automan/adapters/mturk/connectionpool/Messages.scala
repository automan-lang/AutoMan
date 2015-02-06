package edu.umass.cs.automan.adapters.mturk.connectionpool

import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Thunk

protected[MTurk] class Message
protected[MTurk] case class AcceptReq[A <: Answer](t: Thunk[A]) extends Message
protected[MTurk] case class BudgetReq() extends Message
protected[MTurk] case class CancelReq[A <: Answer](t: Thunk[A]) extends Message
protected[MTurk] case class DisposeQualsReq(q: MTurkQuestion) extends Message
protected[MTurk] case class EnqueuedHIT[A <: Answer](ts: List[Thunk[A]], exclude_worker_ids: List[String]) extends Message
protected[MTurk] case class RejectReq[A <: Answer](t: Thunk[A]) extends Message
protected[MTurk] case class RetrieveReq[A <: Answer](ts: List[Thunk[A]]) extends Message
//protected[MTurk] case class TimeoutReq[A <: Answer](ts: List[Thunk[A]]) extends Message
