package edu.umass.cs.automan.adapters.Mock

import java.util.Date

import edu.umass.cs.automan.adapters.Mock.events.AnswerPool
import edu.umass.cs.automan.adapters.Mock.question._
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Thunk}

object MockAdapter {
  def apply(init: MockAdapter => Unit) : MockAdapter = {
    val a = new MockAdapter
    init(a)                     // assign values to fields in anonymous constructor
    a.init()                    // run superclass initializer with values from previous line
    a
  }
}

// note that the default is NOT to use memoization in testing
class MockAdapter extends AutomanAdapter {
  var _answers: List[AnswerPool] = List.empty
  private var _mock_funds : BigDecimal = 5.00
  private var _state : MockState = _
  private var _quantum_length_sec : Int = 30
  _use_memoization = false

  // setters and getters
  def mock_funds_=(b: BigDecimal) { _mock_funds = b }
  def mock_funds = _mock_funds
  def answer_trace_=(answers: List[AnswerPool]) { _answers = answers }
  def answer_trace = _answers
  def quantum_length_sec = _quantum_length_sec
  def quantum_length_sec_=(sec: Int) { _quantum_length_sec = sec }

  // associated question types
  override type CBQ = MockCheckboxQuestion
  override type CBDQ = MockCheckboxDistributionQuestion
  override type FTQ = MockFreeTextQuestion
  override type FTDQ = MockFreeTextDistributionQuestion
  override type RBQ = MockRadioButtonQuestion
  override type RBDQ = MockRadioButtonDistributionQuestion

  // type factories
  override protected def CBQFactory(): CBQ = new MockCheckboxQuestion
  override protected def CBDQFactory(): CBDQ = new MockCheckboxDistributionQuestion
  override protected def FTQFactory(): FTQ = new MockFreeTextQuestion
  override protected def FTDQFactory(): FTDQ = new MockFreeTextDistributionQuestion
  override protected def RBQFactory(): RBQ = new MockRadioButtonQuestion
  override protected def RBDQFactory(): RBDQ = new MockRadioButtonDistributionQuestion

  // DSL
  def Option(id: Symbol, text: String) = MockOption(id, text)

  // API
  override protected[automan] def cancel[A <: Answer](t: Thunk[A]): Thunk[A] = {
    t.copy_as_cancelled()
  }
  override protected[automan] def reject[A <: Answer](t: Thunk[A]): Thunk[A] = {
    t.copy_as_rejected()
  }
  override protected[automan] def retrieve[A <: Answer](ts: List[Thunk[A]]): List[Thunk[A]] = {
    assert(ts.count(_.state != SchedulerState.RUNNING) == 0)
    synchronized {
      // for each thunk, pair with answer, updating MockState as we go
      val (final_ts, updated_state) = ts.foldLeft(List[Thunk[A]](), _state) { case (acc, t) =>
        val (updated_ts, state) = acc
        val (t2: Thunk[A], state2: MockState) = state.answer(t, this.answer_trace)
        (t2 :: updated_ts, state2)
      }

      // return final state after checking for timeouts
      _state = updated_state
      timeout(final_ts)
    }
  }
  private def timeout[A <: Answer](ts: List[Thunk[A]]): List[Thunk[A]] = {
    synchronized {
      val (final_ts, final_state) = ts.foldLeft(List[Thunk[A]](), _state) { case (acc, t) =>
        val (updated_ts, state) = acc
        val (t2: Thunk[A], state2: MockState) =
          if (t.state == SchedulerState.RUNNING && t.expires_at.before(state.current_time)) {
            (t.copy_as_timeout(), state)
          } else {
            (t, state)
          }
        (t2 :: updated_ts, state2)
      }

      // advance virtual clock and return final state
      _state = final_state.advance_clock()
      final_ts
    }
  }
  override protected[automan] def post[A <: Answer](ts: List[Thunk[A]], exclude_worker_ids: List[String]) : List[Thunk[A]] = {
    synchronized {
      // set all thunks to RUNNING and update MockState
      val (final_ts, final_state) = ts.foldLeft(List[Thunk[A]](), _state) { case (acc, t) =>
        // accumulator consists of a list of RUNNING thunks and the latest MockState
        val (updated_ts: List[Thunk[A]], state: MockState) = acc
        // mark this thunk as RUNNING, pair it with the question, and update the MockState
        val (t2, state2) = state.register(t.question, t)
        // add the RUNNING thunk to a RUNNING thunk list and return the updated MockState
        (t2 :: updated_ts, state2)
      }

      _state = final_state
      final_ts
    }
  }
  override protected[automan] def accept[A <: Answer](t: Thunk[A]): Thunk[A] = {
    t.copy_as_accepted()
  }
  override protected[automan] def process_custom_info[A <: Answer](t: Thunk[A], i: Option[String]): Thunk[A] = {
    t.copy_as_processed()
  }
  override protected[automan] def get_budget_from_backend(): BigDecimal = _mock_funds

  override def init(): Unit = {
    val now = new Date()
    _state = MockState(_quantum_length_sec, now, now, Map.empty, Set.empty)
    super.init()
  }
}
