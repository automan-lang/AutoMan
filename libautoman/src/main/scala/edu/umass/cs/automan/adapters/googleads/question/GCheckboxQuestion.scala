package edu.umass.cs.automan.adapters.googleads.question

import java.security.MessageDigest
import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.mock.GCheckboxMockResponse
import edu.umass.cs.automan.adapters.googleads.policy.aggregation.GMinimumSpawnPolicy
import edu.umass.cs.automan.core.question.CheckboxQuestion
import org.apache.commons.codec.binary.Hex

import scala.collection.JavaConverters._
import scala.util.Random

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
    new String(Hex.encodeHex(md.digest(id.toString.getBytes())))
  }

  // private API
  _minimum_spawn_policy = GMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): GCheckboxMockResponse = {
    GCheckboxMockResponse(question_id, response_time, a, worker_id)
  }

  def create(): String = {
    val choices = options.map(_.question_text).toArray
    val urls = options.map(_.image_url).toArray
    // if every choice contains a url, add images to the question
    if (!urls.contains("")) {
      val params = List(form.id, text, other, required, choices, urls).map(_.asInstanceOf[AnyRef]).asJava
      item_id_=(form.addQuestion("checkboxImgs", params))
    }
    else {
      val params = List(form.id, text, other, required, choices).map(_.asInstanceOf[AnyRef]).asJava
      item_id_=(form.addQuestion("checkbox", params))
    }
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
      form.getItemResponses("getCheckboxResponses", item_id, read_so_far)
        .map((s: util.ArrayList[String]) => s.asScala.toList.map(lookup).toSet)
    }
    read_so_far += newResponses.length
    answers_enqueue(newResponses)
  }

  // Queue a bunch (50% 1, 25% 2, 12.5% 3...) of fake answers
  def fakeAnswer(): Unit = {
    def fakeRespond(l : List[A]): List[A] = {
      val fakeSet: Set[Symbol] = options.map(x => options(Random.nextInt(options.length)).question_id).toSet
      if (Random.nextBoolean()) fakeRespond(fakeSet :: l)
      else l
    }
    answers_enqueue(fakeRespond(Nil))
  }
}
