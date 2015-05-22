package edu.umass.cs.automan.adapters.MTurk.connectionpool

import edu.umass.cs.automan.adapters.MTurk.question.MTurkQuestion
import edu.umass.cs.automan.core.scheduler.Task

protected[MTurk] sealed trait Message extends Comparable[Message] {
  protected def order: Int
  override def compareTo(m: Message) = this.order - m.order
}
protected[MTurk] case class ShutdownReq() extends Message {
  override protected def order = 0
}
protected[MTurk] case class AcceptReq[A](t: Task) extends Message {
  override protected def order = 3
}
protected[MTurk] case class BudgetReq() extends Message {
  override protected def order = 1
}
protected[MTurk] case class CancelReq[A](t: Task) extends Message {
  override protected def order = 2
}
protected[MTurk] case class DisposeQualsReq(q: MTurkQuestion) extends Message {
  override protected def order = 4
}
protected[MTurk] case class CreateHITReq[A](ts: List[Task], exclude_worker_ids: List[String]) extends Message {
  override protected def order = 3
}
protected[MTurk] case class RejectReq[A](t: Task, correct_answer: String) extends Message {
  override protected def order = 3
}
protected[MTurk] case class RetrieveReq[A](ts: List[Task]) extends Message {
  override protected def order = 3
}
