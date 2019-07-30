package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.MultiEstimationQuestion
import org.apache.commons.codec.binary.Hex
import scala.collection.JavaConverters._

class GMultiEstimationQuestion extends MultiEstimationQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  override type A = MultiEstimationQuestion#A // Array[Double]

  // public API
  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(item_id.getBytes())))
  }

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: Array[Double], worker_id: UUID): MockResponse = ???

  def create(): String = {
    val fields = dimensions.map(_.id)
    // default required so that all fields must be filled in to be considered one response
    // possible workaround if !required: only consider answers that are all filled in
    val params = List(form.id, text, fields, required = true, fields.length).map(_.asInstanceOf[AnyRef]).asJava
    form.addQuestion("multiEstimation", params)
  }

  def answer(): Unit = ???
}

