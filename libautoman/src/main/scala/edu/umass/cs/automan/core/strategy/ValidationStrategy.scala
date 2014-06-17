package edu.umass.cs.automan.core.strategy

import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.scheduler.Thunk
import edu.umass.cs.automan.core.question.Question
import java.util.UUID

abstract class ValidationStrategy[Q <: Question](question: Q) {
  val _computation_id = UUID.randomUUID()
  var _budget_committed: BigDecimal = 0.00
  var _num_possibilities: BigInt = 2
  var _thunks = List[Thunk]()

  def is_done: Boolean
  def num_possibilities: BigInt = _num_possibilities
  def num_possibilities_=(n: BigInt) { _num_possibilities = n }
  def select_answer : Answer
  def spawn(suffered_timeout: Boolean): List[Thunk]
}