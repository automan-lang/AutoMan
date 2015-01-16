package edu.umass.cs.automan.adapters.Mock

import edu.umass.cs.automan.adapters.Mock.events.Epoch
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.Thunk
import java.util.{UUID,Date}

case class MockState(epoch_index: Int,
                     virtual_time: Date,
                     qu: Map[UUID,Question],
                     answered: Set[Answer]
                      ) {
  // given a thunk and list of answer epochs,
  // pair an answer with a thunk if it is available
  def answer[A <: Answer](t: Thunk[A], epochs: List[Epoch]) : (Thunk[A],MockState) = {
    // get the question id
    val question_id = qu(t.thunk_id).id
    // get the current epoch from the epoch list
    val current_epoch = epochs(epoch_index)
    // get all of the answers that _could_ answer this thunk
    val available: List[(UUID, Answer)] = current_epoch.answers.filter {
      case (id,answer) => id == question_id && !answered.contains(answer)
    }.asInstanceOf[List[(UUID, A)]]

    // if any answers are available, pair the thunk with an answer and return
    // new MockState
    if (available.size > 0) {
      val a = available.head._2.asInstanceOf[A]
      val t2 = t.copy_with_answer(a, UUID.randomUUID().toString)
      val ms = MockState(
        epoch_index,
        virtual_time,
        qu,
        answered + a
      )
      (t2,ms)
    // otherwise, advance the virtual time and the epoch index
    // and check the thunk for timeout
    } else {
      val vt2 = DatePlus(virtual_time, current_epoch.duration_s)
      val t2 = if (t.expires_at.before(vt2)) { t.copy_as_timeout() } else { t }
      (t2, MockState(epoch_index + 1, vt2, qu, answered))
    }
  }
  def register[A <: Answer](q: Question, t: Thunk[A]) : (Thunk[A],MockState) = {
    val ms2 = MockState(
      this.epoch_index,
      this.virtual_time,
      qu + (t.thunk_id -> q),
      answered
    )
    (t.copy_as_running(),ms2)
  }

  private def DatePlus(d: Date, sec: Int) : Date = {
    val d2 = new Date(d.getTime)
    d2.setTime(d.getTime + (sec.toLong * 1000L))
    d2
  }
}