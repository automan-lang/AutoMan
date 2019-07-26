package edu.umass.cs.automan.adapters.googleads.question

import edu.umass.cs.automan.adapters.googleads.ads.{Ad, Campaign}
import edu.umass.cs.automan.adapters.googleads.forms._

trait GQuestion extends edu.umass.cs.automan.core.question.Question {

  protected var _other: Boolean = false
  protected var _required: Boolean = true
  protected var _limit: Boolean = false
  protected var _item_id: String = ""
  protected var _campaign: Option[Campaign] = None
  protected var _ad: Option[Ad] = None
  // defaults for now, will need to change core.DSL
  protected var _ad_title: String = "Assist Crowdsourcing Research"
  protected var _ad_subtitle: String = "Input Your Expertise"
  protected var _ad_descript: String = "Answer just one quick question to assist computer science research"
  protected var _form: Option[Form] = None

  def other_=(o: Boolean) { _other = o }
  def other: Boolean = _other
  def required_=(r: Boolean) { _required = r }
  def required: Boolean = _required
  def limit_=(l: Boolean) { _limit = l }
  def limit: Boolean = _limit
  def item_id: String = _item_id
  def item_id_=(id: String) { _item_id = id }
  def campaign: Campaign = _campaign match { case Some(c) => c case None => throw new UninitializedError}
  def campaign_=(c: Campaign) { _campaign = Some(c) }
  def ad: Ad = _ad match { case Some(a) => a case None => throw new UninitializedError}
  def ad_=(a: Ad) { _ad = Some(a) }
  def form: Form = _form match { case Some(f) => f case None => throw new UninitializedError}
  def form_=(f: Form) { _form = Some(f) }

}
