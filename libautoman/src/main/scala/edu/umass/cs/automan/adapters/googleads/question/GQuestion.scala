package edu.umass.cs.automan.adapters.googleads.question

import com.google.api.services.script.model.ExecutionRequest
import scala.collection.JavaConverters._
import edu.umass.cs.automan.adapters.googleads.ads._
import edu.umass.cs.automan.adapters.googleads.forms._

trait GQuestion extends edu.umass.cs.automan.core.question.Question {
  protected var _question: String = ""
  protected var _choices: List[Choice] = List.empty
  protected var _image_urls: List[String] = List.empty
  protected var _other: Boolean = false
  protected var _required: Boolean = false
  protected var _limit: Boolean = false
  protected var _form_id: String = ""
  protected var _ad: Option[Ad] = None
  protected var _campaign: Option[Campaign] = None
  protected var _form: Option[Form] = None

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
  def ad: Ad = _ad match { case Some(ad) => ad case None => throw new Exception("Ad not initialized") }
  def ad_=(a: Ad) { _ad = Some(a) }
  def campaign: Campaign = _campaign match { case Some(camp) => camp case None => throw new Exception("Campaign not initialized") }
  def campaign_=(c: Campaign) { _campaign = Some(c) }
  def form: Form = _form match { case Some(f) => f case None => throw new Exception("Form not initialized") }
  def form_=(f: Form) { _form = Some(f) }

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
}
