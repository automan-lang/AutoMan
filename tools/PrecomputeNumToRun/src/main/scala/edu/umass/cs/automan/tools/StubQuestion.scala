package edu.umass.cs.automan.tools

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.DiscreteScalarQuestion

class StubQuestion(np: Int) extends DiscreteScalarQuestion {
  override def num_possibilities: BigInt = np

  override protected[automan] def getOutcome(adapter: AutomanAdapter): O = ???

  override def memo_hash: String = ???

  override private[automan] def init_validation_policy(): Unit = ???

  override private[automan] def init_price_policy(): Unit = ???

  override private[automan] def init_timeout_policy(): Unit = ???

  override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: this.A): MockResponse = {
    new MockResponse(question_id, response_time) {
      override def toXML: String = ???
    }
  }

  override protected[automan] def getQuestionType: QuestionType = ???
}
