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
protected[mturk] case class AcceptReq(ts: List[Task]) extends Message {
  override protected def order = 5
}
protected[mturk] case class BudgetReq() extends Message {
  override protected def order = 1
}
protected[mturk] case class CancelReq(ts: List[Task]) extends Message {
  override protected def order = 2
}
protected[mturk] case class DisposeQualsReq(q: MTurkQuestion) extends Message {
  override protected def order = 6
}
protected[mturk] case class CreateHITReq(ts: List[Task], exclude_worker_ids: List[String]) extends Message {
  override protected def order = 3
}
protected[mturk] case class RejectReq(ts_reasons: List[(Task,String)]) extends Message {
  override protected def order = 5
}
protected[mturk] case class RetrieveReq(ts: List[Task], current_time: Date) extends Message {
  override protected def order = 4
}

