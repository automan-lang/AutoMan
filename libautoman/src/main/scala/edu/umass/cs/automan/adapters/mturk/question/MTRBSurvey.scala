package edu.umass.cs.automan.adapters.mturk.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.Survey

import scala.xml.Node

class MTRBSurvey extends Survey with MTurkSurvey {
  override type AA = this.type
  override type O = this.type
  override type R = this.type

  override def description: String = ???

  override def group_id: String = ???

  override protected[mturk] def fromXML(x: Node): Array[Any] = ???

  override protected[mturk] def toXML(randomize: Boolean): Node = ???

  override def memo_hash: String = ???

  override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: Array[Any], worker_id: UUID): MockResponse = ???

  override protected[automan] def prettyPrintAnswer(answer: Array[Any]): String = ???

  override protected[automan] def composeOutcome(o: MTRBSurvey.this.type, adapter: AutomanAdapter): MTRBSurvey.this.type = ???

  override protected[automan] def getQuestionType: QuestionType = ???
}
