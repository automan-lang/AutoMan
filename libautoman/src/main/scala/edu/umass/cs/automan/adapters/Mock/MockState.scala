package edu.umass.cs.automan.adapters.Mock

import edu.umass.cs.automan.adapters.Mock.events.{AnswerPool, UntimedAnswerPool, TimedAnswerPool}
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.Thunk
import java.util.{UUID,Date}

case class MockState(quantum_sec: Int,
                     start_time: Date,
                     current_time: Date,
                     qu: Map[UUID,Question],
                     answered: Set[Answer]
                      ) {
  private def during_timespan(time_start: Date, time_end: Date, time_event: Date) : Boolean = {
//    time_start.after(time_event) &&         // event happens after start (exclusive)
//      (time_end.compareTo(time_event) <= 0) // event happens before end (inclusive)
    val first = time_event.after(time_start)
    val second = time_event.compareTo(time_end) <= 0
    first && second
  }

  // get all of the answers that arrived between
  // the last time we checked and now that have not
  // already been paired with an existing thunk
  private def get_available(answerpools: List[AnswerPool]) : List[(UUID,Answer)] = {
    // the time span we care about goes from the last
    // time that we checked (exclusive) until now (inclusive)
    val window_start = DatePlus(current_time, -quantum_sec)
    val window_end = current_time

    // first, check for timed answers
    val timed_answers = answerpools.map {
      case ta: TimedAnswerPool =>
        val event_time = DatePlus(start_time, ta.occurs_at_time_s)
        if (during_timespan(window_start, window_end, event_time)) {
          Some(ta)
        } else {
          None
        }
      case _ => None
    }.flatten.map { ta => ta.answers }.flatten.filter { pair => !answered.contains(pair._2) }

    // next, get untimed answers
    val untimed_answers = answerpools.map {
      case ta: TimedAnswerPool => None
      case ap@(up: UntimedAnswerPool) => Some(ap)
    }.flatten.map { ta => ta.answers }.flatten.filter { pair => !answered.contains(pair._2) }

    // put timed answers first
    timed_answers ::: untimed_answers
  }

  // given a thunk and list of answer epochs,
  // pair an answer with a thunk if it is available
  def answer[A <: Answer](t: Thunk[A], answerpools: List[AnswerPool]) : (Thunk[A],MockState) = {
    // get the question id
    val question_id = qu(t.thunk_id).id

    // get a list of potential answers
    val available = get_available(answerpools)

    // if any answers are available, pair the thunk with an answer and return
    // new MockState
    if (available.size > 0) {
      val a = available.head._2.asInstanceOf[A]
      val t2 = t.copy_with_answer(a, UUID.randomUUID().toString)
      val ms = MockState(
        quantum_sec,
        start_time,
        current_time,
        qu,
        answered + a
      )
      (t2,ms)
    // otherwise, advance the virtual time
    } else {
      // just return the thunk as-is
      (t, MockState(quantum_sec, start_time, current_time, qu, answered))
    }
  }
  def register[A <: Answer](q: Question, t: Thunk[A]) : (Thunk[A],MockState) = {
    val ms2 = MockState(
      quantum_sec,
      start_time,
      current_time,
      qu + (t.thunk_id -> q),
      answered
    )
    (t.copy_as_running(),ms2)
  }
  def advance_clock() : MockState = {
    val updated_time = DatePlus(current_time, quantum_sec)
    val elapsed = (updated_time.getTime()-start_time.getTime()) / 1000
    Utilities.DebugLog(String.format("Advance virtual time to = %s, %s seconds from start.", updated_time.toString, elapsed.toString), LogLevel.INFO, LogType.ADAPTER, null)
    MockState(
      quantum_sec,
      start_time,
      updated_time,
      qu,
      answered
    )
  }

  // advance date +sec seconds
  private def DatePlus(d: Date, sec: Int) : Date = {
    val d2 = new Date(d.getTime)
    d2.setTime(d.getTime + (sec.toLong * 1000L))
    d2
  }
}