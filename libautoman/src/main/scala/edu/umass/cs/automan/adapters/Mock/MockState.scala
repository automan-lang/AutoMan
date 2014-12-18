package edu.umass.cs.automan.adapters.Mock

import edu.umass.cs.automan.adapters.Mock.question.MockQuestion
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Thunk
import java.util.UUID

case class MockState(qu: Map[UUID,MockQuestion[_ <: Answer]], answered: Set[Answer]) {
  def answer[A <: Answer](t: Thunk[A]) : (Thunk[A],MockState) = {
    val mock_question = qu(t.thunk_id)
    val available = mock_question.mock_answers.filterNot(answered.contains)
    if (available.size > 0) {
      val a = available.head.asInstanceOf[A]
      val t2 = t.copy_with_answer(a, UUID.randomUUID().toString)
      val ms = MockState(
        qu,
        answered + a
      )
      (t2,ms)
    } else {
      (t,this)
    }
  }
  def register[A <: Answer](q: MockQuestion[A], t: Thunk[A]) : (Thunk[A],MockState) = {
    val ms = MockState(
      qu + (t.thunk_id -> q),
      answered
    )
    (t.copy_as_running(),ms)
  }
}