package org.automanlang.adapters.mturk.util

object XML {
  def surveyAnswerFilter(n: scala.xml.Node, id: String): scala.xml.Node = {
    val toRet = (n \\ "Answer").filter { a => // get rid of question form header
      (a \\ "QuestionIdentifier").foldLeft(false) {
        case (acc, q) => acc || q.text == id
      }
    }
    toRet.head
  }
}
