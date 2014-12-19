package edu.umass.cs.automan.adapters.Mock

import edu.umass.cs.automan.adapters.Mock.question._
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Thunk

object MockAdapter {
  def apply() : MockAdapter = MockAdapter(a => Unit)
  def apply(init: MockAdapter => Unit) : MockAdapter = {
    val a = new MockAdapter
    init(a)                     // assign values to fields in anonymous constructor
    a.init()                    // run superclass initializer with values from previous line
    a
  }
}

// note that the default is NOT to use memoization in testing
class MockAdapter extends AutomanAdapter {
  private var _mock_budget : BigDecimal = 0.00
  private var _state : MockState = _
  _use_memoization = false

  // setters and getters
  override def budget_=(b: BigDecimal) { _mock_budget = b }
  override def budget = _mock_budget

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
    synchronized {
      // for each thunk, pair with answer, updating MockState as we go
      val (final_ts, final_state) = ts.foldLeft(List[Thunk[A]](), _state) { case (acc, t) =>
        val (updated_ts, state) = acc
        val (t2, state2) = state.answer(t)
        (t2 :: updated_ts, state2)
      }

      _state = final_state
      final_ts
    }
  }
  override protected[automan] def post[A <: Answer](ts: List[Thunk[A]], exclude_worker_ids: List[String]) : List[Thunk[A]] = {
    synchronized {
      val (final_ts, final_state) = ts.foldLeft(List[Thunk[A]](), _state) { case (acc, t) =>
        val (updated_ts: List[Thunk[A]], state) = acc
        val (t2, state2) = state.register(t.question.asInstanceOf[MockQuestion[A]], t)
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
  override protected[automan] def get_budget_from_backend(): BigDecimal = budget

  override def init(): Unit = {
    _state = MockState(Map.empty, Set.empty)
    super.init()
  }
}
