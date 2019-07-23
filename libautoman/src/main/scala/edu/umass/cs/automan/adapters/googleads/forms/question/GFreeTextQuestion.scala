package edu.umass.cs.automan.adapters.googleads.forms.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.FreeTextQuestion

object GFreeTextQuestion {
  def apply(id: String,
            question: String,
            other: Boolean = false,
            required: Boolean = true,
            limit: Boolean = false): String = {
    val ftq = new GFreeTextQuestion()
    ftq.form_id_=(id)
    ftq.question_=(question)
    ftq.other_=(other)
    ftq.required_=(required)
    ftq.limit_=(limit)
    ftq.create(id, "freeText")
  }
}

class GFreeTextQuestion extends FreeTextQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  //  override type A = RadioButtonQuestion#A

  // public API
  def memo_hash: String = ???


  // private API
  override def toMockResponse(question_id: UUID, response_time: Date, a: String, worker_id: UUID): MockResponse = ???
}
