package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.mock.GCheckboxMockResponse
import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
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
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): GCheckboxMockResponse = {
    GCheckboxMockResponse(question_id, response_time, a, worker_id)
  }

  def create(): String = {
    val choices = options.map(_.question_text).toArray
    val params = List(form.id, text, other, required, choices).map(_.asInstanceOf[AnyRef]).asJava
    item_id_=(form.addQuestion("checkbox", params))
    item_id
  }

  def answer(): Unit = {
    // look for corresponding symbol
    def lookup (str: String): Symbol = {
      options.find(_.question_text == str)
        .get
        .question_id
    }

    val newResponses : List[A] = {
      form.getItemResponses(item_id, read_so_far)
        .map((s: util.ArrayList[String]) => s.asScala.toList.map(lookup).toSet)
    }
    read_so_far += newResponses.length
    answers_enqueue(newResponses)
  }
}
