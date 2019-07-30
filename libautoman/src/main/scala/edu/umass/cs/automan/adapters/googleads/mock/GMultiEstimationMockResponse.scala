package edu.umass.cs.automan.adapters.googleads.mock

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.mock.MockResponse

case class GMultiEstimationMockResponse(question_ids: Array[Symbol], response_time: Date, answers: Array[Double], worker_id: UUID)
  extends MockResponse(UUID.randomUUID(), response_time, worker_id) {
  override def toXML: String = ???
}
