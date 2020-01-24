package edu.umass.cs.automan.adapters.mturk.util

object XML {
  def surveyAnswerFilter(n: scala.xml.Node, id: String) : scala.xml.Node =
    (n \\ "Answer").filter { a =>
      (a \\ "QuestionIdentifier").foldLeft (false){
      case (acc, q) => acc || q.text == id }
    }.head
}
