package edu.umass.cs.automan.adapters.mturk.connectionpool

import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Thunk

protected[mturk] sealed trait Message extends Comparable[Message] {
  protected def order: Int
  override def compareTo(m: Message) = this.order - m.order
}
protected[mturk] case class ShutdownReq() extends Message {
  override protected def order = 0
}
protected[mturk] case class AcceptReq[A](t: Thunk[A]) extends Message {
  override protected def order = 3
}
protected[mturk] case class BudgetReq() extends Message {
  override protected def order = 1
}
protected[mturk] case class CancelReq[A](t: Thunk[A]) extends Message {
  override protected def order = 2
}
protected[mturk] case class DisposeQualsReq(q: MTurkQuestion) extends Message {
  override protected def order = 4
}
protected[mturk] case class CreateHITReq[A](ts: List[Thunk[A]], exclude_worker_ids: List[String]) extends Message {
  override protected def order = 3
}
protected[mturk] case class RejectReq[A](t: Thunk[A]) extends Message {
  override protected def order = 3
}
protected[mturk] case class RetrieveReq[A](ts: List[Thunk[A]]) extends Message {
  override protected def order = 3
}
