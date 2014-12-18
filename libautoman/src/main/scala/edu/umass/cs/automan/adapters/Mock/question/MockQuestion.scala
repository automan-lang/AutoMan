package edu.umass.cs.automan.adapters.Mock.question

import edu.umass.cs.automan.core.answer.Answer

trait MockQuestion[A <: Answer] {
  var _answers: Set[A] = Set.empty
  def mock_answers_=(answers: Set[A]) { _answers = answers }
  def mock_answers = _answers
}
