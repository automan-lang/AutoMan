package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.mock.GEstimationMockResponse
import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.question.EstimationQuestion
import org.apache.commons.codec.binary.Hex

import scala.collection.JavaConverters._

class GEstimationQuestion extends EstimationQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  override type A = EstimationQuestion#A // Double

  // public API
  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(item_id.getBytes())))
  }

  override def cloneWithConfidence(conf: Double): EstimationQuestion = ???

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): GEstimationMockResponse = {
    GEstimationMockResponse(question_id, response_time, a, worker_id)
  }

  def create(): String = {
    val params = List(form.id, text, required, min_value, max_value).map(_.asInstanceOf[AnyRef]).asJava
    item_id_=(form.addQuestion("estimation", params))
    item_id
  }

  def answer(): Unit = {
    val newResponses : List[A] = form.getItemResponses(item_id, read_so_far)
        .map((s: String) => s.toDouble)
    read_so_far += newResponses.length
    answers_enqueue(newResponses)
  }
}
