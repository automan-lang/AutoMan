package edu.umass.cs.automan.adapters.googleads.question

import edu.umass.cs.automan.adapters.googleads.ads.{Ad, Campaign}
import edu.umass.cs.automan.adapters.googleads.forms._
import scala.collection.mutable

trait GQuestion extends edu.umass.cs.automan.core.question.Question {

  protected[googleads] var _campaign: Option[Campaign] = None
  protected[googleads] var _ad: Option[Ad] = None
  // defaults for now, will need to change core.DSL
  protected var _ad_title: String = "Assist Crowdsourcing Research"
  protected var _ad_subtitle: String = "Input Your Expertise"
  protected var _ad_descript: String = "Answer just one quick question to assist computer science research"
  protected var _english: Boolean = false

  protected[googleads] var _form: Option[Form] = None
  protected var _form_descript: String = "This question is part of ongoing computer science research at Williams College.\n" +
    "All answers are fully anonymous and we will not collect any personal information.\n" +
    "Please answer only once.\n" +
    "To find out more about Williams Computer Science visit https://csci.williams.edu/"
  protected var _other: Boolean = false
  protected var _required: Boolean = true
  protected var _item_id: String = ""

  protected var _answers: mutable.Queue[A] = mutable.Queue.empty
  // number of answers retrieved from backend so far
  protected[question] var read_so_far: Int = 0

  def other_=(o: Boolean) { _other = o }
  def other: Boolean = _other
  def required_=(r: Boolean) { _required = r }
  def required: Boolean = _required
  def item_id: String = _item_id
  def item_id_=(id: String) { _item_id = id }
  def campaign: Campaign = _campaign match { case Some(c) => c case None => throw new UninitializedError }
  def campaign_=(c: Campaign) { _campaign = Some(c) }
  def ad: Ad = _ad match { case Some(a) => a case None => throw new UninitializedError }
  def ad_=(a: Ad) { _ad = Some(a) }
  def ad_title: String = _ad_title
  def ad_title_=(at: String) { _ad_title = at }
  def ad_subtitle: String = _ad_subtitle
  def ad_subtitle_=(as: String) { _ad_subtitle = as }
  def ad_descript: String = _ad_descript
  def ad_descript_=(des: String) { _ad_descript = des }
  def english: Boolean = _english
  def english_=(e: Boolean) { _english = e }
  def form: Form = _form match { case Some(f) => f case None => throw new UninitializedError }
  def form_=(f: Form) { _form = Some(f) }
  def form_descript: String = _form_descript
  def form_descript_=(fd: String) { _form_descript = fd }
  def answers: mutable.Queue[A] = _answers
  def answers_=(a: mutable.Queue[A]) { _answers = a }

  def answers_enqueue(l: List[A]): Unit = l.foreach(answers.enqueue(_))
  def answers_dequeue(): Option[A] = if(answers.isEmpty) {None} else {Some(answers.dequeue())}

  // to be implemented by each question type
  def create(): String
  // queue up new responses from the backend to be processed
  def answer(): Unit

}