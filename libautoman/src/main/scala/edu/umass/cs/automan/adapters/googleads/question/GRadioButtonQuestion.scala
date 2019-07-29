package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util.{Date, UUID}
import org.apache.commons.codec.binary.Hex
import scala.collection.JavaConverters._
import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.adapters.googleads.mock.RadioButtonMockResponse
import edu.umass.cs.automan.core.question.RadioButtonQuestion

//object GRadioButtonQuestion {
//  def apply(id: String,
//            question: String,
//            options: List[GQuestionOption],
//            other: Boolean = false,
//            required: Boolean = true,
//            limit: Boolean = false): String = {
//    val mcq = new GRadioButtonQuestion()
//    mcq.form_id_=(id)
//    mcq.text_=(question)
//    mcq.options_=(options)
//    mcq.other_=(other)
//    mcq.required_=(required)
//    mcq.limit_=(limit)
//    if (!options.map(_.image_url).contains(""))
//      mcq.create(id, "radioButtonImgs")
//    else
//      mcq.create(id, "radioButton")
//  }
//}

class GRadioButtonQuestion extends RadioButtonQuestion with GQuestion {
  override type QuestionOptionType = GQuestionOption
  override type A = RadioButtonQuestion#A

  // public API
  override def randomized_options: List[QuestionOptionType] = {
    form.shuffle()
    options
  }

  // don't know how this works
  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(create().getBytes())))
  }

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): RadioButtonMockResponse = {
    RadioButtonMockResponse(question_id, response_time, a, worker_id)
  }

  def create(): String = {
    val choices = options.map(_.question_text).toArray
    val images = options.map(_.image_url).toArray
    // if there are urls, add images to question
    if (!images.contains("")) {
      val params = List(form.id, text, other, required, choices, images).map(_.asInstanceOf[AnyRef]).asJava
      form.addQuestion("radioButtonImgs", params)
    }
    else {
      val params = List(form.id, text, other, required, choices).map(_.asInstanceOf[AnyRef]).asJava
      form.addQuestion("radioButton", params)
    }
  }

  def answer(): Unit = {
    val s = form.getItemResponses(item_id, read_so_far).map { s =>
      options.find(o => o.question_text == s).get.question_id
    }
    read_so_far += s.length
    answers_enqueue(s)
  }
}
