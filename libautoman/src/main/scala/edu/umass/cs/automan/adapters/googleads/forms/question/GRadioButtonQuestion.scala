package edu.umass.cs.automan.adapters.googleads.forms.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.RadioButtonQuestion

object GRadioButtonQuestion {
  def apply(id: String,
            question: String,
            choices: List[Choice],
            other: Boolean = false,
            required: Boolean = true,
            limit: Boolean = false): String = {
    val mcq = new GRadioButtonQuestion()
    mcq.form_id_=(id)
    mcq.question_=(question)
    mcq.choices_=(choices)
    mcq.other_=(other)
    mcq.required_=(required)
    mcq.limit_=(limit)
    if (!choices.map(_.url).contains(""))
      mcq.create(id, "radioButtonImgs")
    else
      mcq.create(id, "radioButton")
  }
}

class GRadioButtonQuestion extends RadioButtonQuestion with GQuestion {
  override type QuestionOptionType = GQuestionOption

  // public API
  override def randomized_options: List[QuestionOptionType] = ???

  override def memo_hash: String = ???

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override protected def toMockResponse(question_id: UUID, response_time: Date, a: Symbol, worker_id: UUID): MockResponse = ???
}
