package edu.umass.cs.automan.adapters.googleads

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.googleads.ads.Account
import edu.umass.cs.automan.adapters.googleads.question._
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.logging.{DebugLog, LogLevelInfo, LogType, Memo}
import edu.umass.cs.automan.core.question.Question
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

import scala.collection.immutable
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
  // toggle sandbox mode
  var test = false

  def production_account_id: Long = _production_account_id match {
    case Some(id) => id
    case None => throw new Exception("GoogleAdsAdapter production account id")
  }

  def production_account_id_=(id: Long) {
    _production_account_id = Some(id)
  }

  def production_account: Account = _production_account match {
    case Some(c) => c
    case None => throw new Exception("GoogleAdsAdapter production account")
  }

  private def setup(): Unit = {
    if (!test) _production_account = try {
      Some(Account(production_account_id))
    } catch {
      case _: Throwable => None
    }
  }

  protected[automan] def accept(ts: List[Task]): Option[List[Task]] = {
    Some(ts.map(t => {
      if (!test) t.question.asInstanceOf[GQuestion].campaign.delete()
      t.copy_as_accepted()
    }))
  }

  protected[automan] def backend_budget(): Option[BigDecimal] = Some(Int.MaxValue)

  protected[automan] def cancel(ts: List[Task], toState: SchedulerState.Value): Option[List[Task]] = {
    val stateChanger = toState match {
      case SchedulerState.CANCELLED => (t: Task) => t.copy_as_cancelled()
      case SchedulerState.TIMEOUT => (t: Task) => {
        ts.foreach(_.question.asInstanceOf[GQuestion].cpc =
          (ts.head.question.wage * t.worker_timeout / 3600).setScale(2, RoundingMode.CEILING))
        t.copy_as_timeout()
      }
      case SchedulerState.DUPLICATE => (t: Task) => t.copy_as_duplicate()
      case _ => throw new Exception(s"Invalid target state $toState for cancellation request.")
    }
    if (!test) ts.foreach(t => t.question.asInstanceOf[GQuestion].campaign.delete())
    Some(ts.map(stateChanger))
  }

  protected[automan] def post(ts: List[Task], exclude_worker_ids: List[String]): Option[List[Task]] = {
    // create form, campaign, and ad

    def taskPost(t: Task): Task = {
      val q = t.question.asInstanceOf[GQuestion]
      this.synchronized {
        if (!test) q.post(production_account)
      }
      t.copy_as_running()
    }

    Some(ts.map(t =>
      t.state match {
        case SchedulerState.READY =>
          ts.foreach(_.question.asInstanceOf[GQuestion].cpc =
            (ts.head.question.wage * t.worker_timeout / 3600).setScale(2, RoundingMode.CEILING))
          taskPost(t)
        case _ => throw new Exception(s"Invalid target state ${t.state} for post request.")
      }
    ))
  }

  protected[automan] def reject(ts_reasons: List[(Task, String)]): Option[List[Task]] =
    Some(ts_reasons.map(
      { case (t: Task, _: String) =>
        if (!test) t.question.asInstanceOf[GQuestion].campaign.delete()
        t.copy_as_rejected()
      }
    )
    )

  protected[automan] def retrieve(ts: List[Task], current_time: Date): Option[List[Task]] = {

    // get unique questions and one task
    val qMap: Map[GQuestion, Task] = ts
      .map(t => t.question.asInstanceOf[GQuestion] -> t).groupBy(_._1).map({ case (k, v) => (k, v.head._2) })

    //Just questions
    val qSet = qMap.keys.toSet

      if (!test) qSet.foreach { q =>
        if (!q.isApproved) {
          DebugLog("Ad awaiting approval", LogLevelInfo(), LogType.ADAPTER, q.id)
          Thread.sleep(10000) //wait 10s //TODO: q._update_frequency does nothing, but this should probably go in core
        } else
          q.answer()
      }
      else qSet.foreach(q => q.fakeAnswer())

    //Get and queue answers for each question
    def answer(t: Task): Task = {
      val q = t.question.asInstanceOf[GQuestion]
      val updatedT = q.answers_dequeue() match {
        case Some(a) => t.copy_with_answer(a, UUID.randomUUID().toString)
        case None => t
      }
      updatedT
    }

    val answeredTasks: List[Task] = ts.map(t =>
      t.state match {
        case SchedulerState.RUNNING => answer(t)
        case _ => throw new Exception(s"Invalid target state ${t.state} for retrieve request.")
      }
    )

    val remaining: List[Task] = qMap.flatMap({case (q: GQuestion, t: Task) =>
      var l: List[Task] = Nil
      var a: Option[q.A] = q.answers_dequeue()
      while(a.isDefined) {
        val _t = t.copy(UUID.randomUUID())
        l = _t.copy_with_answer(a.get, UUID.randomUUID().toString) :: l
        a = q.answers_dequeue()
      }
      l
    }).toList

    Some(answeredTasks ++ remaining)
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
