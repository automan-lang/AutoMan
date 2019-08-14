package edu.umass.cs.automan.adapters.googleads.question

import edu.umass.cs.automan.adapters.googleads.ads.{Account, Ad, Campaign}
import edu.umass.cs.automan.adapters.googleads.forms._
import edu.umass.cs.automan.core.logging.{DebugLog, LogLevelInfo, LogType}

import scala.collection.mutable

trait GQuestion extends edu.umass.cs.automan.core.question.Question {

  protected var _cpc: BigDecimal = 0.4
  protected var _ad_title: String = "Assist Crowdsourcing Research"
  protected var _ad_subtitle: String = "Input Your Expertise"
  protected var _ad_description: String = "Answer just one quick question to assist science research"
  protected var _ad_keywords: Set[String] = Set.empty
  protected var _english_only: Boolean = false
  protected var _us_only: Boolean = false
  protected var _male_only: Boolean = false
  protected var _female_only: Boolean = false
  protected var _form_description: String = ""
  protected var _item_id: String = ""
  protected var _other: Boolean = false
  protected var _required: Boolean = true
  protected var _answers: mutable.Queue[A] = mutable.Queue.empty

  // number of answers retrieved from backend so far
  protected[question] var read_so_far: Int = 0
  protected[googleads] var _form: Option[Form] = None
  protected[googleads] var _campaign: Option[Campaign] = None
  protected[googleads] var _ad: Option[Ad] = None

  def ad_title: String = _ad_title
  def ad_title_=(at: String) { _ad_title = at }
  def ad_subtitle: String = _ad_subtitle
  def ad_subtitle_=(as: String) {  _ad_subtitle = as }
  def ad_description: String = _ad_description
  def ad_description_=(des: String) { _ad_description = des }
  def ad_keywords: Set[String] = _ad_keywords
  def ad_keywords_=(words: Set[String]) { _ad_keywords = words }
  def cpc: BigDecimal = _cpc
  def cpc_=(c: BigDecimal) {_cpc = c}
  def english_only: Boolean = _english_only
  def english_only_=(e: Boolean) { _english_only = e }
  def us_only: Boolean = _us_only
  def us_only_=(us: Boolean) { _us_only = us }
  def male_only: Boolean = _male_only
  def male_only_=(m: Boolean) { _male_only = m }
  def female_only: Boolean = _female_only
  def female_only_=(f: Boolean) { _female_only = f }
  def form_description: String = _form_description
  def form_description_=(fd: String) { _form_description = fd }
  def item_id: String = _item_id
  def item_id_=(id: String) { _item_id = id }
  def other: Boolean = _other
  def other_=(o: Boolean) { _other = o }
  def required: Boolean = _required
  def required_=(r: Boolean) { _required = r }
  def answers: mutable.Queue[A] = _answers
  def answers_=(a: mutable.Queue[A]) { _answers = a }

  def form: Form = _form match {
    case Some(f) => f;
    case None => throw new UninitializedError
  }
  def form_=(f: Form) { _form = Some(f) }
  def campaign: Campaign = _campaign match {
    case Some(c) => c;
    case None => throw new UninitializedError
  }
  def campaign_=(c: Campaign) { _campaign = Some(c) }
  def ad: Ad = _ad match {
    case Some(a) => a;
    case None => throw new UninitializedError
  }
  def ad_=(a: Ad) { _ad = Some(a) }

  def create(): String
  def answer(): Unit // queue up new responses from the backend to be processed
  def fakeAnswer(): Unit

  def answers_enqueue(l: List[A]): Unit = l.foreach(answers.enqueue(_))
  def answers_dequeue(): Option[A] = {
    if (answers.isEmpty) None; else Some(answers.dequeue())
  }

  // create form, campaign, and ad if none currently exist
  def post(acc: Account): Unit = {
    _form match {
      case Some(_) =>
      case None =>
        form = Form(title)
        form.setDescription(form_description)

        _image_url match {
          case None =>
          case Some(url) => form.addImage(url)
        }
        create()
    }

    campaign = _campaign match {
      case Some(c) => c
      case None => acc.createCampaign(budget, title, id)
    }

    ad = _ad match {
      case Some(a) => a
      case None =>
        val a = campaign.createAd(ad_title, ad_subtitle, ad_description, form.getPublishedUrl, ad_keywords.toList, cpc)
        campaign.setCPC(cpc)
        if (english_only) campaign.englishOnly()
        if (us_only) campaign.usOnly()
        if (male_only) campaign.maleOnly()
        if (female_only) campaign.femaleOnly()
        while (!a.is_approved) {
          DebugLog("Ad awaiting approval", LogLevelInfo(), LogType.ADAPTER, id)
          Thread.sleep(5 * 1000) // 5 seconds should prevent rate limit
        }
        a
    }
  }

    def isApproved: Boolean = {
      if (ad.is_approved) true
      else false
    }
}