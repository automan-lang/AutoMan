package edu.umass.cs.automan.adapters.mturk.connectionpool

import java.util.Date

import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.scheduler.Task

protected[mturk] sealed trait Message extends Comparable[Message] {
  protected def order: Int
  override def compareTo(m: Message) = this.order - m.order
}
protected[mturk] case class ShutdownReq() extends Message {
  override protected def order = 0
}
protected[mturk] case class AcceptReq(t: Task) extends Message {
  override protected def order = 4
}
protected[mturk] case class BudgetReq() extends Message {
  override protected def order = 2
}
protected[mturk] case class CancelReq(t: Task) extends Message {
  override protected def order = 3
}
protected[mturk] case class DisposeQualsReq(q: MTurkQuestion) extends Message {
  override protected def order = 5
}
protected[mturk] case class CreateHITReq(ts: List[Task], exclude_worker_ids: List[String]) extends Message {
  override protected def order = 4
}
protected[mturk] case class RejectReq(t: Task, correct_answer: String) extends Message {
  override protected def order = 4
}
protected[mturk] case class RetrieveReq(ts: List[Task], current_time: Date) extends Message {
  override protected def order = 4
}

