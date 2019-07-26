package edu.umass.cs.automan.adapters.googleads.question

import java.util.{Date, UUID}

import scala.collection.JavaConverters._
import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.RadioButtonQuestion

import scala.collection.mutable

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
  override def randomized_options: List[QuestionOptionType] = ???

  override def memo_hash: String = ???

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override protected def toMockResponse(question_id: UUID, response_time: Date, a: Symbol, worker_id: UUID): MockResponse = ???

  def create(): String = {
    val params = List(form.id, text, other, required, limit, options.toArray).map(_.asInstanceOf[AnyRef]).asJava
    form.addQuestion("radioButton", params)
  }

  def answer() = {
    val s = form.getItemResponses(item_id, read_so_far).map { s =>
      options.find(o => o.question_text == s).get.question_id
    }
    read_so_far += s.length
    answers_enqueue(s)
  }
}
