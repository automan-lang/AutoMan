package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.RadioButtonVectorQuestion
import org.apache.commons.codec.binary.Hex
import scala.collection.JavaConverters._

class GRadioButtonVectorQuestion extends RadioButtonVectorQuestion with GQuestion {
  type QuestionOptionType = GQuestionOption
  override type A = RadioButtonVectorQuestion#A // Symbol

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
  override def toMockResponse(question_id: UUID, response_time: Date, a: Symbol, worker_id: UUID): MockResponse = ???

  def create(): String = {
    val choices = options.map(_.question_text).toArray
    val images = options.map(_.image_url).toArray
    // if there are urls, add images to question
    // TODO: images not working correctly
    if (!images.contains("")) {
      val params = List(form.id, text, other, required, choices, images).map(_.asInstanceOf[AnyRef]).asJava
      item_id_=(form.addQuestion("radioButtonImgs", params))
    }
    else {
      val params = List(form.id, text, other, required, choices).map(_.asInstanceOf[AnyRef]).asJava
      item_id_=(form.addQuestion("radioButton", params))
    }
    item_id
  }

  def answer(): Unit = {
    def lookup (str: String): Symbol = {
      options.find(_.question_text == str)
        .get
        .question_id
    }
    val newResponses : List[A] = form.getItemResponses[String](item_id, read_so_far).map(lookup)
    read_so_far += newResponses.length
    answers_enqueue(newResponses)
  }
}
