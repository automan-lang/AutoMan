package edu.umass.cs.automan.adapters.googleads.question

import com.google.api.services.script.model.{ExecutionRequest, Operation}

import scala.collection.JavaConverters._
import edu.umass.cs.automan.adapters.googleads.forms._
import edu.umass.cs.automan.adapters.googleads.util.Service._

trait GQuestion extends edu.umass.cs.automan.core.question.Question {
  protected var _question: String = ""
  protected var _choices: List[Choice] = List.empty
  protected var _image_urls: List[String] = List.empty
  protected var _other: Boolean = false
  protected var _required: Boolean = false
  protected var _limit: Boolean = false

  protected var _form_id: String = ""
  protected var _item_id: String = ""

  // public API
  def question_=(q: String) { _question = q }
  def question: String = _question
  def choices_=(c: List[Choice]) { _choices = c }
  def choices: List[Choice] = _choices
  def other_=(o: Boolean) { _other = o }
  def other: Boolean = _other
  def required_=(r: Boolean) { _required = r }
  def required: Boolean = _required
  def limit_=(l: Boolean) { _limit = l }
  def limit: Boolean = _limit

  def form_id: String = _form_id
  def form_id_=(id: String) { _form_id = id }

  def item_id: String = _item_id
  def item_id_=(id: String) { _item_id = id }

  // returns the item (question) id
  def create(form_id: String, question_type: String): String = {
    val options = choices.map(_.question_text)
    val params = question_type match {
      case "checkbox" | "radioButton" =>
        List(form_id, question, other, required, limit, options.toArray).map(_.asInstanceOf[AnyRef]).asJava
      case "checkboxImgs" | "radioButtonImgs" =>
        val urls = choices.map(_.url)
        List(form_id, question, other, required, limit, options.toArray, urls.toArray).map(_.asInstanceOf[AnyRef]).asJava
      case "freeText" | "estimation" =>
        List(form_id, question, required, limit).map(_.asInstanceOf[AnyRef]).asJava
    }
    val q_request = new ExecutionRequest()
      .setFunction(question_type)
      .setParameters(params)
    val q_op = service.scripts()
      .run(script_id, q_request)
      .execute()

    println(s"$question_type question '$question' created")

    q_op.getResponse.get("result").toString
  }

    def retrieve[T]: List[A] = {
    val request: ExecutionRequest = new ExecutionRequest()
      .setFunction("getItemResponses")
      .setParameters(List(id.asInstanceOf[AnyRef],item_id.asInstanceOf[AnyRef]).asJava)

    val op: Operation = service.scripts()
      .run(script_id, request)
      .execute()

      val err = op.getError
      val ret = err match {
        case null => op.getResponse.get("result").asInstanceOf[java.util.ArrayList[T]].asScala.toList
        case _ => throw ScriptError(err.getMessage)
      }

      //TODO: make work
      //only cases for radiobutton and estimate
      ret match {
        case _ : List[java.util.ArrayList[_]] => ret.map(_.asInstanceOf[A])
        case _ : List[T] => ret.asInstanceOf[List[A]]
      }
  }
}
