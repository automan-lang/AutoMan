package edu.umass.cs.automan.adapters.mturk.worker

import java.util.concurrent.PriorityBlockingQueue
import java.util.Date
import com.amazonaws.mturk.service.axis.RequesterService
import edu.umass.cs.automan.adapters.mturk.logging.MTMemo
import edu.umass.cs.automan.adapters.mturk.mock.MockRequesterService
import edu.umass.cs.automan.adapters.mturk.question.MTurkQuestion
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Task}

class TurkWorker(val backend: RequesterService, val sleep_ms: Int, val mock_service: Option[MockRequesterService], val memo_handle: MTMemo) {
  type HITID = String
  type BatchKey = (String,BigDecimal,Int)   // (group_id, cost, timeout); uniquely identifies a batch
  type HITKey = (BatchKey, String)          // (BatchKey, memo_hash); uniquely identifies a HIT

  // work queue
  private val _requests = new PriorityBlockingQueue[FIFOMessage]()

  // response data
  private val _responses = scala.collection.mutable.Map[Message, Any]()

  // worker exit condition
  var _workerExitState: Option[Throwable] = None

  // worker
  startWorker()

  // API
  def accept(ts: List[Task]) : Option[List[Task]] = {
    blocking_enqueue[List[Task]](AcceptReq(ts))
  }
  def backend_budget: Option[BigDecimal] = {
    blocking_enqueue[BigDecimal](BudgetReq())
  }
  def cancel(ts: List[Task], toState: SchedulerState.Value) : Option[List[Task]] = {
    blocking_enqueue[List[Task]](CancelReq(ts, toState))
  }
  def cleanup_qualifications() : Unit = {
    blocking_enqueue(DisposeQualsReq())
  }
  def post(ts: List[Task], exclude_worker_ids: List[String]) : Option[List[Task]] = {
    blocking_enqueue[List[Task]](CreateHITReq(ts, exclude_worker_ids))
  }
  def reject(ts_reasons: List[(Task, String)]) : Option[List[Task]] = {
    blocking_enqueue[List[Task]](RejectReq(ts_reasons))
  }
  def retrieve(ts: List[Task], current_time: Date) : Option[List[Task]] = {
    blocking_enqueue[List[Task]](RetrieveReq(ts, current_time))
  }
  def shutdown(): Unit = {
    nonblocking_enqueue(ShutdownReq())
  }

  // IMPLEMENTATIONS
  private def nonblocking_enqueue(req: Message) : Unit = this.synchronized {
    // put job in queue
    _requests.add(new FIFOMessage(req))
  }

  /**
    * Schedule a task on the MTurk worker thread. Returns Some[T]
    * when everything goes OK.  Returns None when the worker thread
    * crashed, signaling that the callee should cleanup their own
    * resources and shutdown their thread.
    * @param req The request.
    * @tparam T The return type.
    * @return Some return value or None on failure.
    */
  private def blocking_enqueue[T](req: Message) : Option[T] = {
    // wait for response
    // while loop is because the JVM is
    // permitted to send spurious wakeups;
    // also enture that backend is still running
    while(this.synchronized { !_responses.contains(req) }
          && _workerExitState.isEmpty) {
      var enqueued = false
      // Note that the purpose of this second lock
      // is to provide blocking semantics
      req.synchronized {
        // enqueue inside sync so that we don't miss notify
        if (!enqueued) {
          nonblocking_enqueue(req)
          enqueued = true
        }
        req.wait() // release lock and block until notify is sent
      }
    }

    // return output
    this.synchronized {
      // check that loop did not end due to fatal error
      _workerExitState match {
        case None => {
          val ret = _responses(req).asInstanceOf[T]
          _responses.remove(req)
          Some(ret)
        }
        case Some(throwable) => None
      }

    }
  }
  private def startWorker() : Thread = {
    val t = initWorkerThread()
    t.start()
    t
  }
  private def initWorkerThread(): Thread = {
    DebugLog("No worker thread; starting one up.", LogLevelInfo(), LogType.ADAPTER, null)
    val t = new Thread(new WorkerRunnable(this, _requests, _responses))
    t.setName("MTurk Worker Thread")
    t
  }
}