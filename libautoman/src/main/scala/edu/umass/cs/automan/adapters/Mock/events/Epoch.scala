package edu.umass.cs.automan.adapters.Mock.events

import java.util.UUID
import edu.umass.cs.automan.core.answer.Answer

case class Epoch(duration_s: Int, answers: List[(UUID,Answer)])