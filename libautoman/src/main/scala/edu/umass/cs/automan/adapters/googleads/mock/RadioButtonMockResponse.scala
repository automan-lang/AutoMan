package edu.umass.cs.automan.adapters.googleads.mock

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.mock.MockResponse

case class RadioButtonMockResponse(question_id: UUID, response_time: Date, answer: Symbol, worker_id: UUID)
  extends MockResponse(question_id, response_time, worker_id) {
  override def toXML: String = ???
}
