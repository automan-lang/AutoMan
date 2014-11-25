package edu.umass.cs.automan.adapters.MTurk.connectionpool

import edu.umass.cs.automan.adapters.MTurk.question.MTurkQuestion
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Thunk

protected[MTurk] case class EnqueuedHIT[A <: Answer](ts: List[Thunk[A]], dual: Boolean, exclude_worker_ids: List[String])
protected[MTurk] case class RetrieveReq[A <: Answer](ts: List[Thunk[A]])
protected[MTurk] case class BudgetReq()
protected[MTurk] case class DisposeQualsReq(q: MTurkQuestion)