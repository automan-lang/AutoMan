package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.mock.GFreeTextMockResponse
import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.adapters.googleads.util.KeywordList
import edu.umass.cs.automan.core.question.FreeTextVectorQuestion
import org.apache.commons.codec.binary.Hex

import scala.collection.JavaConverters._
import scala.util.Random

class GFreeTextVectorQuestion extends FreeTextVectorQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  override type A = FreeTextVectorQuestion#A // String

  // public API
  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(item_id.getBytes())))
  }

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): GFreeTextMockResponse = {
    GFreeTextMockResponse(question_id, response_time, a, worker_id)
  }

  def create(): String = {
    val params = List(form.id, text, required).map(_.asInstanceOf[AnyRef]).asJava
    item_id_=(form.addQuestion("freeText", params))
    item_id
  }

  def answer(): Unit = {
    val newResponses : List[A] = form.getItemResponses[A](item_id, read_so_far)
    read_so_far += newResponses.length
    answers_enqueue(newResponses)
  }

  //Queue a bunch (50% 1, 25% 2, 12.5% 3...) of fake answers
  def fakeAnswer(): Unit = {
    def fakeRespond(l : List[A]): List[A] = {
      if (Random.nextBoolean()) fakeRespond(KeywordList.randomWord :: l)
      else l
    }
    answers_enqueue(fakeRespond(Nil))
  }
}