package edu.umass.cs.automan.adapters.Mock.question

import edu.umass.cs.automan.core.question.QuestionOption

case class MockOption(override val question_id: Symbol, override val question_text: String)
  extends QuestionOption(question_id: Symbol, question_text: String)