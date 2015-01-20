package edu.umass.cs.automan.adapters.Mock.events

import java.util.UUID
import edu.umass.cs.automan.core.answer.Answer

// indicates which answers occur at how many seconds into the simulation
case class TimedAnswer(occurs_at_time_s: Int, answers: List[(UUID,Answer)]) {
  assert(occurs_at_time_s > 0)
}