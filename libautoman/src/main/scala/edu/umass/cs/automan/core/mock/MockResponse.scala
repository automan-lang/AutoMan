package edu.umass.cs.automan.core.mock

import java.util.{Calendar, UUID, Date}

import edu.umass.cs.automan.core.util.Utilities

abstract class MockResponse(question_id: UUID, response_time: Date) {
  def toXML: String
  def responseTime: Calendar = Utilities.dateToCalendar(response_time)
}