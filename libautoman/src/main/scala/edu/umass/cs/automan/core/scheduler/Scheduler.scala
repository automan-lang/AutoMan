package edu.umass.cs.automan.core.scheduler

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.info.{EpochInfo, QuestionInfo}
import edu.umass.cs.automan.core.memoizer.{ThunkLogger, AutomanMemoizer}
import edu.umass.cs.automan.core.answer.{Answer, ScalarAnswer}
import scala.collection.mutable
import scala.collection.mutable.Queue
import edu.umass.cs.automan.core.question.{ScalarQuestion, Question}
import edu.umass.cs.automan.core.{LogLevel, LogType, Utilities, AutomanAdapter}
import edu.umass.cs.automan.core.exception.OverBudgetException

class Scheduler (val question: Question,
                 val adapter: AutomanAdapter,
                 val memoizer: Option[AutomanMemoizer],
                 val thunklog: Option[ThunkLogger],
                 val poll_interval_in_s: Int,
                 val virtual_time: Option[Date]) {
  def this(question: Question,
           adapter: AutomanAdapter,
           memoizer: Option[AutomanMemoizer],
           thunklog: Option[ThunkLogger],
           poll_interval_in_s: Int) = this(question, adapter, memoizer, thunklog, poll_interval_in_s, None)
  type A = question.A
  type B = question.B
  // we need to get the initialized strategy instance from
  // the Question itself in order to satisfy the type checker
  private val _strategy = question.strategy_instance
  private var _thunks = scala.collection.immutable.Map[UUID,Thunk[A]]()
  private var _total_needed : Option[Int] = None
  private var _spent: BigDecimal = 0
  private var _epochs: List[EpochInfo] = List.empty
  private var _current_epoch_thunks: List[Thunk[A]] = List.empty

  def post_new_thunks(thunks: List[Thunk[A]], last_iteration_timeout: Boolean, memo_answers: Queue[A]) : List[Thunk[A]] = {
    val num_answers = answered_thunks(thunks).size

    // spawn new thunks; in READY state here
    val new_thunks = _strategy.spawn(thunks, last_iteration_timeout) // OverBudgetException should be thrown here

    _total_needed = Some(new_thunks.size + num_answers)

    // before posting, pick answers out of memo DB
    val recalled_thunks = recall(new_thunks, memo_answers) // blocks

    // remaining new thunks
    val rem_new_thunks = new_thunks.filter{ t => recalled_thunks.forall(_.thunk_id != t.thunk_id)}

    // post remaining thunks
    val posted_thunks = if (!question.dry_run && rem_new_thunks.size > 0) {
      post(rem_new_thunks) // non-blocking
    } else {
      List.empty
    }

    recalled_thunks ::: posted_thunks
  }

  def state : Option[QuestionInfo] = {
    // if the user happens to ask for state
    // before the scheduler has done anything,
    // return None
    synchronized {
      _total_needed match {
        case Some(num_needed) => {
          // make a snapshot
          val my_thunks: Map[UUID, Thunk[A]] = _thunks


          // find the oldest thunk
          val oldest_thunk = my_thunks.values.toArray.sortWith {
            (a, b) =>
              a.created_at.compareTo(b.created_at) < 0
          }.head

          val conf = question match {
            case s: ScalarQuestion => s.confidence
            case _ => 1.0
          }

          Some(
            new QuestionInfo(
              question.id,
              question.title,
              question.title,
              question.text,
              question.question_type,
              oldest_thunk.created_at,
              conf,
              my_thunks.values.toList,
              num_needed,
              question.budget,
              _spent,
              question.dont_reject,
              _epochs
            )
          )
        }
        case None => None
      }
    }
  }

  private def archiveEpoch(ts: List[Thunk[A]], needed_agreement: Int, largest_agreement: Int) : EpochInfo = {
    // make sure that all thunks are completed
    assert(ts.count(_.completed_at == None) == 0)

    // get epoch start time
    val start_time = ts.sortWith((a,b) => a.created_at.compareTo(b.created_at) < 0).head.created_at

    // get epoch end time
    val end_time = ts.sortWith((a,b) => a.completed_at.get.compareTo(b.completed_at.get) > 0).head.created_at

    // create epoch object
    EpochInfo(start_time, end_time, ts, needed_agreement, largest_agreement)
  }

  private def largestAnswerGroup(ts: List[Thunk[A]]) : Int = {
    val answered = ts.filter(_.answer != None)
    if (answered.size == 0) {
      0
    } else {
      answered
        .groupBy(_.answer.get.comparator)
        .values.map { ts_group => ts_group.size }.max
    }
  }

  def run() : B = {
    // check memo DB first
    val memo_answers = get_memo_answers()
    val unpaid_timeouts = new mutable.HashSet[Thunk[A]]()
    var last_iteration_timeout = false
    var over_budget = false
    Utilities.DebugLog("Found " + memo_answers.size + " saved Answers in database.", LogLevel.INFO, LogType.SCHEDULER, question.id)

    try {
      Utilities.DebugLog("Entering scheduling loop...", LogLevel.INFO, LogType.SCHEDULER, question.id)
      Utilities.DebugLog(String.format("Initial budget set at: %s", Utilities.decimalAsDollars(question.budget)), LogLevel.INFO, LogType.SCHEDULER, question.id)
      // run startup hook
      if(!question.dry_run) {
        adapter.question_startup_hook(question)
      }
      while(!_strategy.is_done(_thunks.values.toList)) {
        if (running_thunks(_thunks).size == 0) {
          synchronized {
            val old_needed = _total_needed match { case Some(n) => n; case None => 0 }

            val new_thunks = post_new_thunks(_thunks.values.toList, last_iteration_timeout, memo_answers)

            // this marks the beginning of a new epoch;
            // archive last epoch thunks to EpochInfo
            // and replace current thunk list
            if (_current_epoch_thunks.size != 0) {
              _epochs = archiveEpoch(_current_epoch_thunks, old_needed, largestAnswerGroup(_thunks.values.toList)) :: _epochs
            }

            // update thunk data structures
            _current_epoch_thunks = new_thunks
            _thunks = _thunks ++ new_thunks.map { t => t.thunk_id -> t}
          }
        }

        // ask the backend for answers and memoize_answers
        // conditional covers the case where all thunk answers are recalled from memoDB
        if (running_thunks(_thunks).size > 0) {
          // get data
          // TODO fix: adapter.retrieve() also cancels timed-out thunks, which violates single-responsibility
          val results =
            if (!question.dry_run) {
              adapter.retrieve(running_thunks(_thunks)) // blocks
            } else {
              List.empty
            }

          // atomically update thunk data structures with new results
          synchronized {
            _thunks = _thunks ++ results.map { t => t.thunk_id -> t}
            _current_epoch_thunks = _current_epoch_thunks.map { t => _thunks(t.thunk_id)}
          }

          // check for timed-out thunks
          if (results.count(_.state == SchedulerState.TIMEOUT) != 0) {
            // unreserve these amounts from our budget
            val new_timeouts = results.filter{ t =>
              t.state == SchedulerState.TIMEOUT &&
              !unpaid_timeouts.contains(t)
            }
            _strategy.unpay_for_thunks(new_timeouts)
            last_iteration_timeout = true
          } else {
            last_iteration_timeout = false
          }
          // saves *recently* RETRIEVED thunks
          memoize_answers(results.asInstanceOf[List[Thunk[A]]])  // blocks
        }

        // sleep if necessary
        // TODO: this sleep may no longer be necessary
//        if (!_strategy.is_done(_thunks.values.toList) && incomplete_thunks(_thunks).size > 0) Thread.sleep(poll_interval_in_s * 1000)
      }
      // run shutdown hook
      if(!question.dry_run) {
        adapter.question_shutdown_hook(question)
      }

    } catch {
      case o:OverBudgetException[_] => {
        Utilities.DebugLog("OverBudgetException.", LogLevel.FATAL, LogType.SCHEDULER, question.id)
        over_budget = true
      }
    }
    Utilities.DebugLog("Exiting scheduling loop...", LogLevel.INFO, LogType.SCHEDULER, question.id)

    synchronized {
      val answer = _strategy.select_answer(_thunks.values.toList)

      if (!over_budget) {
        _spent = accept_and_reject(_thunks, answer.asInstanceOf[B])
        Utilities.DebugLog("Total spent: " + _spent.toString(), LogLevel.INFO, LogType.SCHEDULER, question.id)
      } else {
        throw new OverBudgetException(Some(answer))
      }
      question.final_cost = _spent
      answer.asInstanceOf[B]
    }
  }

  def get_memo_answers() : Queue[A] = {
    val answers = Queue[A]()
    answers ++= (memoizer match {
      case Some(m) => m.checkDB(question).map{ a => a.asInstanceOf[A]}
      case None => List()
    })
    answers
  }

  def accept_and_reject(ts: Map[UUID, Thunk[A]], answer: B) : BigDecimal = {
    var spent: BigDecimal = 0
    val accepts = _strategy.thunks_to_accept(ts.values.toList)
    val rejects = _strategy.thunks_to_reject(ts.values.toList)
    val cancels = ts.values.filter(_.state == SchedulerState.CANCELLED)
    val timeouts = ts.values.filter(_.state == SchedulerState.TIMEOUT)

    // Accept
    accepts.foreach { t =>
      Utilities.DebugLog(
        "Accepting thunk " + t.thunk_id +  " with answer \"" + t.answer.get.toString + "\" and paying $" +
          t.cost.toString(), LogLevel.INFO, LogType.SCHEDULER, question.id
      )
      val t2 = adapter.accept(t)
      thunklog match {
        case Some(tl) => tl.writeThunk(t2, SchedulerState.ACCEPTED, t2.worker_id.get)
        case None => Unit
      }
      spent += t.cost
    }

    // Reject
    if (!question.dont_reject) {
      rejects.foreach { t =>
        Utilities.DebugLog(
          "Rejecting thunk " + t.thunk_id + " with incorrect answer \"" +
            t.answer.get.toString + "\"", LogLevel.INFO, LogType.SCHEDULER, question.id
        )
        val t2 = adapter.reject(t)
        thunklog match {
          case Some(tl) => tl.writeThunk(t2, SchedulerState.REJECTED, t2.worker_id.get)
          case None => Unit
        }
      }
    } else {
      // Accept if the user specified "don't reject"
      rejects.foreach { t =>
        Utilities.DebugLog(
          "Accepting (NOT rejecting) thunk " + t.thunk_id + " with incorrect answer \"" +
            t.answer.get.toString + "\". If you did not want this, set Question.dont_reject to false.",
          LogLevel.INFO, LogType.SCHEDULER, question.id
        )
        val t2 = adapter.accept(t)
        thunklog match {
          case Some(tl) => tl.writeThunk(t2, SchedulerState.ACCEPTED, t2.worker_id.get)
          case None => Unit
        }
        spent += t2.cost
      }
    }

    // Early cancel
    cancels.foreach { t =>
      Utilities.DebugLog("Cancelling RUNNING thunk " + t.thunk_id + "...", LogLevel.INFO, LogType.SCHEDULER, question.id)
      adapter.cancel(t)
      thunklog match {
        case Some(tl) => tl.writeThunk(t, SchedulerState.CANCELLED, null)
        case None => Unit
      }
    }

    // Timeouts
    timeouts.foreach { t =>
      thunklog match {
        case Some(tl) => tl.writeThunk(t, SchedulerState.TIMEOUT, null)
        case None => Unit
      }
    }

    spent
  }

  def answered_thunks(ts: List[Thunk[A]]) : List[Thunk[A]] = ts.filter( t =>
    t.state == SchedulerState.PROCESSED &&
    t.state == SchedulerState.RETRIEVED ||
    t.state == SchedulerState.ACCEPTED ||
    t.state == SchedulerState.REJECTED
  ).toList
  def completed_thunks(ts: Map[UUID,Thunk[A]]) : List[Thunk[A]] = ts.values.filter( t =>
    t.state == SchedulerState.PROCESSED &&
    t.state == SchedulerState.RETRIEVED ||
    t.state == SchedulerState.ACCEPTED ||
    t.state == SchedulerState.REJECTED ||
    t.state == SchedulerState.TIMEOUT  ||
    t.state == SchedulerState.CANCELLED
  ).toList
  def incomplete_thunks(ts: Map[UUID,Thunk[A]]) : List[Thunk[A]] = ts.values.filter( t =>
    t.state != SchedulerState.PROCESSED &&
    t.state != SchedulerState.RETRIEVED &&
    t.state != SchedulerState.ACCEPTED &&
    t.state != SchedulerState.REJECTED &&
    t.state != SchedulerState.TIMEOUT &&
    t.state != SchedulerState.CANCELLED
  ).toList
  def memoize_answers(ts: List[Thunk[A]]) {
    // save
    memoizer match {
      case Some(m) =>
        ts.filter(_.state == SchedulerState.RETRIEVED).foreach {t =>
          m.writeAnswer(question, t.answer.get)
        }
      case None => Unit
    }
  }
  def post(ts: List[Thunk[A]]) : List[Thunk[A]] = {
    Utilities.DebugLog("Posting " + ts.size + " tasks.", LogLevel.INFO, LogType.SCHEDULER, question.id)
    adapter.post(ts, question.blacklisted_workers)
  }
  def running_thunks(ts: Map[UUID,Thunk[A]]) = ts.values.filter(_.state == SchedulerState.RUNNING).toList
  def recall(ts: List[Thunk[A]], answers: Queue[A]) : List[Thunk[A]] = {
    // sanity check
    assert(ts.forall(_.state == SchedulerState.READY))

    if (answers.size > 0) {
      ts.take(answers.size).map { t =>
        val answer = answers.dequeue()
        Utilities.DebugLog("Pairing thunk " + t.thunk_id + " with memoized answer \"" + answer.toString + "\".", LogLevel.INFO, LogType.SCHEDULER, question.id)
        val worker_id = answer.worker_id
        val t2 = t.copy_with_answer(answer, worker_id)
        t2.question.blacklist_worker(worker_id)
        adapter.process_custom_info(t2, answer.custom_info)
      }
    } else {
      List.empty
    }
  }
}