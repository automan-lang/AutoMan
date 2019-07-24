package edu.umass.cs.automan.adapters.googleads

import java.util.Date

import edu.umass.cs.automan.adapters.googleads.ads.{Account, Campaign}
import edu.umass.cs.automan.adapters.googleads.forms.Form
import edu.umass.cs.automan.adapters.googleads.question._
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.question.QuestionOption
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

object GoogleAdsAdapter {
  def apply(init: GoogleAdsAdapter => Unit): GoogleAdsAdapter = {
    val gaa : GoogleAdsAdapter = new GoogleAdsAdapter
    init(gaa)
    gaa.init()
    gaa.setup()
    gaa
  }
}

class GoogleAdsAdapter extends AutomanAdapter {
  override type CBQ = GCheckboxQuestion
  override type CBDQ = GCheckboxVectorQuestion
  override type MEQ = GMultiEstimationQuestion
  override type EQ = GEstimationQuestion
  override type FTQ = GFreeTextQuestion
  override type FTDQ = GFreeTextVectorQuestion
  override type RBQ = GRadioButtonQuestion
  override type RBDQ = GRadioButtonVectorQuestion
//  override type MemoDB =

  private var _production_account_id: Option[Long] = None
  private var _budget: Option[BigDecimal] = None
  private var _cpc : Option[BigDecimal] = None
  private var _english_only : Boolean = false
  private var _title : Option[String] = None


  private var _production_account: Option[Account] = None
  private var _campaign: Option[Campaign] = None
  private var _form : Option[Form] = None


  def production_account_id: Long = _production_account_id match {case Some(id) => id; case None => throw new Exception("googleadsadapter account id")}
  def production_account_id_=(id: Long) {_production_account_id = Some(id)}
  def budget : BigDecimal = _budget match {case Some(id) => id; case None => throw new Exception("googleadsadapter budget")}
  def budget_=(b: BigDecimal) {_budget = Some(b)}
  def title : String = _title match {case Some(t) => t; case None => throw new Exception("googleadsadapter title")}
  def title_=(t: String) {_title = Some(t)}

  def production_account: Account = _production_account_id match {case Some(c) => c; case None => throw new Exception("googleadsadapter production account")}
  def campaign: Campaign = _campaign match {case Some(c) => c; case None => throw new Exception("googleadsadapter campaign")}
  def form : Form = _form match {case Some(f) => f; case None => throw new Exception("googleadsadapter form")}

  private def setup(): Unit = {
        _production_account = try {Some(Account(production_account_id))} catch {case _ : Throwable => None}
        _campaign = try {Some(production_account.createCampaign(budget,title))} catch {case _ : Throwable => None}
        if(_english_only) campaign.restrictEnglish()
        _form = try {Some(Form(title))} catch {case _ : Throwable => None}
    }
  /**
    * Tell the backend to accept the answer associated with this ANSWERED task.
    *
    * @param ts ANSWERED tasks.
    * @return Some ACCEPTED tasks if successful.
    */
  override protected def accept(ts: List[Task]): Option[List[Task]] = ???

  /**
    * Get the budget from the backend.
    *
    * @return Some budget if successful.
    */
  override protected def backend_budget(): Option[BigDecimal] = Some(campaign.budget_amount)

  /**
    * Cancel the given tasks.
    *
    * @param ts      A list of tasks to cancel.
    * @param toState Which scheduler state tasks should become after cancellation.
    * @return Some list of cancelled tasks if successful.
    */
  override protected def cancel(ts: List[Task], toState: SchedulerState.Value): Option[List[Task]] = {
    val stateChanger = toState match {
      case SchedulerState.CANCELLED => (t : Task) => t.copy_as_cancelled()
      case SchedulerState.TIMEOUT => (t : Task) => t.copy_as_timeout()
      case SchedulerState.DUPLICATE => (t : Task) => t.copy_as_duplicate()
      case _ => throw new Exception(s"Invalid target state ${toState} for cancellation request.")
    }
    ts.foreach(t => t.question.)
    ts.map(stateChanger)
  }

  /**
    * Post tasks on the backend, one task for each task.  All tasks given should
    * be marked READY. The method returns the complete list of tasks passed
    * but with new states. Blocking. Invariant: the size of the list of input
    * tasks == the size of the list of the output tasks.
    *
    * @param ts                 A list of new tasks.
    * @param exclude_worker_ids Worker IDs to exclude, if any.
    * @return Some list of the posted tasks if successful.
    */
  override protected def post(ts: List[Task], exclude_worker_ids: List[String]): Option[List[Task]] = ???

  /**
    * Tell the backend to reject the answer associated with this ANSWERED task.
    *
    * @param ts_reasons A list of pairs of ANSWERED tasks and their rejection reasons.
    * @return Some REJECTED tasks if succesful.
    */
  override protected def reject(ts_reasons: List[(Task, String)]): Option[List[Task]] = ???

  /**
    * Ask the backend to retrieve answers given a list of RUNNING tasks. Invariant:
    * the size of the list of input tasks == the size of the list of the output
    * tasks. The virtual_time parameter is ignored when not running in simulator mode.
    *
    * @param ts           A list of RUNNING tasks.
    * @param current_time The current virtual time.
    * @return Some list of RUNNING, RETRIEVED, or TIMEOUT tasks if successful.
    */
  override protected def retrieve(ts: List[Task], current_time: Date): Option[List[Task]] = ???

  def Option(id: Symbol, text: String) = new GQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new GQuestionOption(id, text, image_url)

  protected def CBQFactory()  = new GCheckboxQuestion
  protected def CBDQFactory() = new GCheckboxVectorQuestion
  protected def MEQFactory()  = new GMultiEstimationQuestion
  protected def EQFactory()   = new GEstimationQuestion
  protected def FTQFactory()  = new GFreeTextQuestion
  protected def FTDQFactory() = new GFreeTextVectorQuestion
  protected def RBQFactory()  = new GRadioButtonQuestion
  protected def RBDQFactory() = new GRadioButtonVectorQuestion
  override protected def MemoDBFactory(): MemoDB = ???
}
