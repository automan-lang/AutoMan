package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.CheckboxQuestion
import org.apache.commons.codec.binary.Hex
import scala.collection.JavaConverters._

class GCheckboxQuestion extends CheckboxQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  override type A = CheckboxQuestion#A // Set[Symbol]

  // public API
  override def randomized_options: List[QuestionOptionType] = {
    form.shuffle()
    options
  }

  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(item_id.getBytes())))
  }


  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override protected def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): MockResponse = ???

  def create(): String = {
    val params = List(form.id, text, other, required, options.toArray).map(_.asInstanceOf[AnyRef]).asJava
    form.addQuestion("checkbox", params)
  }

  def answer(): Unit = ???
}
