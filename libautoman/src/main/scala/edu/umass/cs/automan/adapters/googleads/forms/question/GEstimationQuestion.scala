package edu.umass.cs.automan.adapters.googleads.forms.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.EstimationQuestion

object GEstimationQuestion {
  def apply(id: String,
            question: String,
            other: Boolean = false,
            required: Boolean = true,
            limit: Boolean = false): String = {
    val eq = new GEstimationQuestion()
    eq.form_id_=(id)
    eq.question_=(question)
    eq.other_=(other)
    eq.required_=(required)
    eq.limit_=(limit)
    eq.create(id, "estimation")
  }
}

class GEstimationQuestion extends EstimationQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  //  override type A = RadioButtonQuestion#A

  // public API
  def memo_hash: String = ???

  override def cloneWithConfidence(conf: Double): EstimationQuestion = ???

  // private API
  override def toMockResponse(question_id: UUID, response_time: Date, a: Double, worker_id: UUID): MockResponse = ???
}
