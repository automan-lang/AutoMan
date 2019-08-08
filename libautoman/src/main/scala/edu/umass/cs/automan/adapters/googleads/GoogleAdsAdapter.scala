package edu.umass.cs.automan.adapters.googleads

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.ads.Account
import edu.umass.cs.automan.adapters.googleads.question._
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

import scala.math.BigDecimal.RoundingMode

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
  override type MemoDB = Memo

  private var _production_account_id: Option[Long] = None
  private var _production_account: Option[Account] = None
  val test = true

  def production_account_id: Long = _production_account_id match {
    case Some(id) => id;
    case None => throw new Exception("googleadsadapter account id")
  }

  def production_account_id_=(id: Long) {
    _production_account_id = Some(id)
  }

  def production_account: Account = _production_account match {
    case Some(c) => c;
    case None => throw new Exception("googleadsadapter production account")
  }

  private def setup(): Unit = {
    if (!test) _production_account = try {
      Some(Account(production_account_id))
    } catch {
      case _: Throwable => None
    }
  }

  /**
    * Tell the backend to accept the answer associated with this ANSWERED task.
    *
    * @param ts ANSWERED tasks.
    * @return Some ACCEPTED tasks if successful.
    */
  protected[automan] def accept(ts: List[Task]): Option[List[Task]] =
    Some(
      ts.map(t => {
        if (!test) t.question.asInstanceOf[GQuestion].campaign.delete()
        t.copy_as_accepted()
      })
    )

  /**
    * Get the budget from the backend.
    *
    * @return Some budget if successful.
    */
  protected[automan] def backend_budget(): Option[BigDecimal] = Some(Int.MaxValue)

  /**
    * Cancel the given tasks.
    *
    * @param ts      A list of tasks to cancel.
    * @param toState Which scheduler state tasks should become after cancellation.
    * @return Some list of cancelled tasks if successful.
    */
  protected[automan] def cancel(ts: List[Task], toState: SchedulerState.Value): Option[List[Task]] = {
    val stateChanger = toState match {
      case SchedulerState.CANCELLED => (t: Task) => t.copy_as_cancelled()
      case SchedulerState.TIMEOUT => (t: Task) => {
        ts.foreach(_.question.asInstanceOf[GQuestion].cpc = (ts.head.question.wage * t.worker_timeout / 3600).setScale(2,RoundingMode.CEILING))
        t.copy_as_timeout()
      }
      case SchedulerState.DUPLICATE => (t: Task) => t.copy_as_duplicate()
      case _ => throw new Exception(s"Invalid target state $toState for cancellation request.")
    }
    if (!test) ts.foreach(t => t.question.asInstanceOf[GQuestion].campaign.delete())
    Some(ts.map(stateChanger))
  }

  /**
    * Post tasks on the backend, one task for each task.  All tasks given should
    * be marked READY. The method returns the complete list of tasks passed
    * but with new states. Blocking. Invariant: the size of the list of input
    * tasks == the size of the list of the output tasks.
    *
    * @param ts                 A list of new tasks.
    * @param exclude_worker_ids Worker IDs to exclude, if any. Not used here.
    * @return Some list of the posted tasks if successful.
    */
  protected[automan] def post(ts: List[Task], exclude_worker_ids: List[String]): Option[List[Task]] = {
    // create campaign, ad, form
    def taskPost(t: Task): Task = {
      val q = t.question.asInstanceOf[GQuestion]
      if (!test) q.post(production_account)
      t.copy_as_running()
    }

    Some(ts.map(t =>
      t.state match {
        case SchedulerState.READY => {
          ts.foreach(_.question.asInstanceOf[GQuestion].cpc = (ts.head.question.wage * t.worker_timeout / 3600).setScale(2,RoundingMode.CEILING))
          taskPost(t)
        }
        case _ => throw new Exception(s"Invalid target state ${t.state} for post request.")
      }
    ))
  }

  /**
    * Tell the backend to reject the answer associated with this ANSWERED task.
    *
    * @param ts_reasons A list of pairs of ANSWERED tasks and their rejection reasons.
    * @return Some REJECTED tasks if succesful.
    */
  protected[automan] def reject(ts_reasons: List[(Task, String)]): Option[List[Task]] =
    Some(
      ts_reasons.map(
        { case (t: Task, s: String) =>
          if (!test) t.question.asInstanceOf[GQuestion].campaign.delete()
          t.copy_as_rejected()
        }
      )
    )

  /**
    * Ask the backend to retrieve answers given a list of RUNNING tasks. Invariant:
    * the size of the list of input tasks == the size of the list of the output
    * tasks. The virtual_time parameter is ignored when not running in simulator mode.
    *
    * @param ts           A list of RUNNING tasks.
    * @param current_time The current virtual time.
    * @return Some list of RUNNING, RETRIEVED, or TIMEOUT tasks if successful.
    */
  protected[automan] def retrieve(ts: List[Task], current_time: Date): Option[List[Task]] = {
    // get unique questions, update answer queue for each
    val qSet: Set[Question] = Set(ts.map(_.question): _*)
    if (!test) {
      qSet.foreach(_.asInstanceOf[GQuestion].answer())
    }
    else {
      println(qSet.head.asInstanceOf[GQuestion].cpc)
      //qSet.foreach(_.asInstanceOf[GQuestion].fakeAnswer())
    }

    def answer(t: Task): Task = {
      val q = t.question.asInstanceOf[GQuestion]
      val updatedT = q.answers_dequeue() match {
        case Some(a) => t.copy_with_answer(a, UUID.randomUUID().toString)
        case None => t
      }
      updatedT
    }


    Some(ts.map(t =>
      t.state match {
        case SchedulerState.RUNNING => println(t.worker_timeout); answer(t)
        case _ => throw new Exception(s"Invalid target state ${t.state} for retrieve request.")
      }
    ))
  }

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

  override protected def MemoDBFactory(): MemoDB = new Memo(_log_config, _database_path, _in_mem_db)
}
