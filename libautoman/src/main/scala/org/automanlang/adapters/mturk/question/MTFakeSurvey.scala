package org.automanlang.adapters.mturk.question

import org.apache.commons.codec.binary.Hex
import org.automanlang.core.question.FakeSurvey

import java.security.MessageDigest
import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.xml.Node

class MTFakeSurvey extends FakeSurvey with MTurkQuestion {
  override type Q = MTurkQuestion

  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }

  override def description: String = _description match {
    case Some(d) => d;
    case None => this.title
  }

  override def group_id: String = title

  /**
   * Parses answer from XML
   *
   * @param x the XML
   * @return Answer value
   */
  override protected[mturk] def fromXML(x: Node): A = {
    val map = collection.mutable.Map[UUID, Q]()
    for (q <- questions) {
      map(q.id) = q
    }

    // parse <QuestionIdentifier> and match with questions(i).id
    // note that we have edited trait MTurkQuestion to also extend Question

    val ans = new ListBuffer[Any]()
    val qs = x \\ "Answer"
    qs.foreach { q =>
      val ques = map(UUID.fromString((q \\ "QuestionIdentifier").text))
      ans += ques.fromXML(q)
    }

    ans.toList
  }

  /**
   * Converts question to standalone XML QuestionForm
   * Calls XMLBody
   *
   * @param randomize Randomize option order?
   * @return XML
   */
  override protected[mturk] def toXML(randomize: Boolean): Node = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      {toQuestionXML(randomize)}
    </QuestionForm>
  }


  /**
   * Helper function to convert question into XML fragment.
   * Not called directly.
   *
   * @param randomize Randomize option order?
   * @return XML
   */
  override protected[mturk] def toQuestionXML(randomize: Boolean): Seq[Node] = {
    questions.flatMap(
      _.toQuestionXML(randomize)
    )
  }

  /**
   * Converts question into a fragment suitable for embedding inside
   * MTSurvey XML output.  Not called directly.
   *
   * @param randomize
   * @return
   */
  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = ???
}
