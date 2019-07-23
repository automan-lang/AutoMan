package edu.umass.cs.automan.adapters.googleads.forms.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.CheckboxQuestion

object GCheckboxQuestion {
  def apply(id: String,
            question: String,
            choices: List[Choice],
            other: Boolean = false,
            required: Boolean = true,
            limit: Boolean = false): String = {
    val cbq = new GCheckboxQuestion()
    cbq.form_id_=(id)
    cbq.question_=(question)
    cbq.choices_=(choices)
    cbq.other_=(other)
    cbq.required_=(required)
    cbq.limit_=(limit)
    if (!choices.map(_.url).contains(""))
      cbq.create(id, "checkboxImgs")
    else
      cbq.create(id, "checkbox")
  }
}

class GCheckboxQuestion extends CheckboxQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption

  // public API
  def memo_hash: String = ???

  override def randomized_options: List[QuestionOptionType] = ???

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override protected def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): MockResponse = ???

}