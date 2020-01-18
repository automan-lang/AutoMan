package edu.umass.cs.automan.adapters.mturk.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.answer.Outcome
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.{Question, Survey}
import org.apache.commons.codec.binary.Hex

import scala.xml.{Node, NodeSeq}

class MTSurvey extends Survey with MTurkQuestion {

  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }

  override protected[mturk] def fromXML(x: Node): (String, Question#A) = ???

  override protected[mturk] def toXML(randomize: Boolean): Node = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      XMLBody(randomize)
    </QuestionForm>
  }

  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    val concat = _question_list.foldLeft("")((acc, o: Outcome[_]) => {
      acc + o.question.memo_hash
    })
    new String(Hex.encodeHex(md.digest(concat.getBytes)))
  }

  override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: (String, Question#A), worker_id: UUID): MockResponse = ???

  // prints answers grouped by worker
  override protected[automan] def prettyPrintAnswer(answer: (String, Question#A)): String = {
    val (_,(question_id,ans)) = answer
    s"${question_id}: ${ans}"
  }

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def XMLBody(randomize: Boolean): Seq[Node] = {
    _question_list.asInstanceOf[List[MTurkQuestion]].map(_.toXML(randomize))
  }

}
