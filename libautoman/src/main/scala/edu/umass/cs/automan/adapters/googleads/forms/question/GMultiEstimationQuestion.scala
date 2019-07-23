package edu.umass.cs.automan.adapters.googleads.forms.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.MultiEstimationQuestion

class GMultiEstimationQuestion extends MultiEstimationQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  //  override type A = RadioButtonQuestion#A

  // public API
  def memo_hash: String = ???

  // private API
  override def toMockResponse(question_id: UUID, response_time: Date, a: Array[Double], worker_id: UUID): MockResponse = ???
}

