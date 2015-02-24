package edu.umass.cs.automan.adapters.mturk.question

sealed trait MTQuestionGroup {
  def group_id: String
}
case class MTRadioButtonQuestionGroup(group_id: String) extends MTQuestionGroup