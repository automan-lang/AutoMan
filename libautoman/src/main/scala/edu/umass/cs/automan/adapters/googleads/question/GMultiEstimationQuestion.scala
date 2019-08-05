package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.mock.GMultiEstimationMockResponse
import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.question.MultiEstimationQuestion
import org.apache.commons.codec.binary.Hex

import scala.collection.JavaConverters._
import scala.util.Random

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
  // TODO: GMultiEstimationMockResponse takes in a question_id: Array[Symbol]
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): GMultiEstimationMockResponse = ???

  def create(): String = {
    val fields = dimensions.map(_.id.toString.drop(1))
    // default required so that all fields must be filled in to be considered one response
    // possible workaround if !required: only consider answers that are all filled in
    val params = List(form.id, text, fields, true, cardinality).map(_.asInstanceOf[AnyRef]).asJava
    item_id_=(form.addQuestion("multiEstimation", params))
    item_id
  }

  def answer(): Unit = {
    val newResponses : List[A] = {
      form.getMultiResponses(item_id, read_so_far, cardinality)
        .map((s: util.ArrayList[String]) => s.asScala.toArray.map(_.toDouble))
    }
    read_so_far += newResponses.length
    answers_enqueue(newResponses)
  }

  //Queue a bunch (50% 1, 25% 2, 12.5% 3...) of fake answers
  def fakeAnswer(): Unit = {
    def fakeRespond(l : List[A]): List[A] = {
      val fakeArray: Array[Double] = dimensions.map(x => Random.nextDouble())
      if (Random.nextBoolean()) fakeRespond(fakeArray :: l)
      else l
    }
    answers_enqueue(fakeRespond(Nil))
  }
}

