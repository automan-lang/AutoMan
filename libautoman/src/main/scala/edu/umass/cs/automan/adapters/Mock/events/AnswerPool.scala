package edu.umass.cs.automan.adapters.Mock.events

import java.util.UUID
import edu.umass.cs.automan.core.answer.Answer

abstract class AnswerPool(answers: List[(UUID,Answer)])

// a collection of answers that can be drawn from anytime
case class UntimedAnswerPool(answers: List[(UUID,Answer)]) extends AnswerPool(answers)

// a collection of answers that is made available at a specific time
case class TimedAnswerPool(occurs_at_time_s: Int, answers: List[(UUID,Answer)]) extends AnswerPool(answers) {
  assert(occurs_at_time_s > 0)
}