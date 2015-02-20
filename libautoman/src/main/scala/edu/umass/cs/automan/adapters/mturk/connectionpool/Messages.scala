package edu.umass.cs.automan.adapters.mturk.connectionpool

import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Thunk

protected[mturk] class Message
protected[mturk] case class AcceptReq[A](t: Thunk[A]) extends Message
protected[mturk] case class BudgetReq() extends Message
protected[mturk] case class CancelReq[A](t: Thunk[A]) extends Message
protected[mturk] case class DisposeQualsReq(q: MTurkQuestion) extends Message
protected[mturk] case class EnqueuedHIT[A](ts: List[Thunk[A]], exclude_worker_ids: List[String]) extends Message
protected[mturk] case class RejectReq[A](t: Thunk[A]) extends Message
protected[mturk] case class RetrieveReq[A](ts: List[Thunk[A]]) extends Message
//protected[mturk] case class TimeoutReq[A](ts: List[Thunk[A]]) extends Message
