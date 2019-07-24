package edu.umass.cs.automan.adapters.googleads.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.FreeTextVectorQuestion

class GFreeTextVectorQuestion extends FreeTextVectorQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption

  // public API
  def memo_hash: String = ???

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: String, worker_id: UUID): MockResponse = ???
}