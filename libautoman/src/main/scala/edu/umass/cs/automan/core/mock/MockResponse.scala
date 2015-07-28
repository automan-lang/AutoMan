package edu.umass.cs.automan.core.mock

import java.util.{UUID, Date}

abstract class MockResponse(question_id: UUID, response_time: Date) {
  def toXML: String
}