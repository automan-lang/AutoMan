package edu.umass.cs.automan.adapters.googleads.forms.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.CheckboxVectorQuestion

class GCheckboxVectorQuestion extends CheckboxVectorQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption

  // public API
  def memo_hash: String = ???

  override def randomized_options: List[QuestionOptionType] = ???

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: Set[Symbol], worker_id: UUID): MockResponse = ???
}