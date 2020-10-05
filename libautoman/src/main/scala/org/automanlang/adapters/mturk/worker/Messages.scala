package org.automanlang.adapters.mturk.worker

import java.util.Date
import org.automanlang.adapters.mturk.question.MTurkQuestion
import org.automanlang.core.scheduler.{SchedulerState, Task}

protected[mturk] sealed trait Message extends Comparable[Message] {
  protected def order: Int
  override def compareTo(m: Message) = this.order - m.order
}
protected[mturk] case class ShutdownReq() extends Message {
  override protected def order = 7
}
protected[mturk] case class AcceptReq(ts: List[Task]) extends Message {
  override protected def order = 5
}
protected[mturk] case class BudgetReq() extends Message {
  override protected def order = 1
}
protected[mturk] case class CancelReq(ts: List[Task], toState: SchedulerState.Value) extends Message {
  override protected def order = 2
}
protected[mturk] case class DisposeQualsReq() extends Message {
  override protected def order = 6
}
protected[mturk] case class CreateHITReq(ts: List[Task], exclude_worker_ids: List[String]) extends Message {
  override protected def order = 3
}
protected[mturk] case class RejectReq(ts_reasons: List[(Task,String)]) extends Message {
  override protected def order = 5
}
protected[mturk] case class RetrieveReq(ts: List[Task], current_time: Date) extends Message {
  override protected def order = 3
}

