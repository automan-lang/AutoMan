package edu.umass.cs.automan.adapters.mturk.question

import java.security.MessageDigest
import java.util.{Date, UUID}
import edu.umass.cs.automan.adapters.mturk.mock.MultiEstimationMockResponse
import edu.umass.cs.automan.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import edu.umass.cs.automan.core.logging.{LogType, LogLevelDebug, DebugLog}
import edu.umass.cs.automan.core.question.{Dimension, MultiEstimationQuestion}
import org.apache.commons.codec.binary.Hex

import scala.xml.NodeSeq

class MTMultiEstimationQuestion(sandbox: Boolean) extends MultiEstimationQuestion with MTurkQuestion {
  override type A = Array[Double]

  _minimum_spawn_policy = MTurkMinimumSpawnPolicy
  private val _action = if (sandbox) {
    "https://workersandbox.mturk.com/mturk/externalSubmit"
  } else {
    "https://www.mturk.com/mturk/externalSubmit"
  }
  private var _iframe_height = 450
  private var _layout: Option[scala.xml.Node] = None

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def description: String = _description match { case Some(d) => d; case None => this.title }
  def layout_=(x: scala.xml.Node) { _layout = Some(x) }
  def layout: scala.xml.Node = _layout match {
    case Some(layout) => layout
    case None => throw new Exception("No layout!")
  }
  def iframe_height_=(height: Int) { _iframe_height = height }
  def iframe_height: Int = _iframe_height
  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }

  // private API
  override def toMockResponse(question_id: UUID, response_time: Date, as: A, worker_id: UUID) : MultiEstimationMockResponse = {
    MultiEstimationMockResponse(dimensions.map(_.id), response_time, as, worker_id)
  }

  def fromXML(x: scala.xml.Node) : A = {
    DebugLog("MTMultiEstimationQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)
    val identifiers = x \\ "Answer" \\ "QuestionIdentifier"
    val answer_map = identifiers
      .map { id =>
        val answer = (x \\ "Answer").filter(a => a.descendant.contains(id)).head
        (Symbol(id.text.drop(1)), (answer \\ "FreeText").text)
      }.toMap
    dimensions.map { dim => answer_map(dim.id).toDouble }
  }

  def renderQuestion(dimension: Dimension) : scala.xml.Node = {
    <p>
      <span>{ dimension.id.toString }</span><br/>
      <input type="text" name={ dimension.id.toString } />
    </p>
  }

  def html() = {
    String.format("<!DOCTYPE html>%n") +
      {
        <html>
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            {
              _layout match {
                case Some(layout) => layout
                case None => NodeSeq.Empty
              }
            }
          </head>
          <body>
            <span id="computation_id" style="display: none">{ id.toString }</span>
            <div id="hit_content">
              <form name="mturk_form" method="post" id="mturk_form" action={_action}>
                <input type="hidden" value={id.toString} name="question_id" id="question_id"/>
                <input type="hidden" value="" name="assignmentId" id="assignmentId"/>
                {
                  _image_url match {
                    case Some(url) => <p><img src={ url }/></p>
                    case None => NodeSeq.Empty
                  }
                }
                {
                  _text match {
                    case Some(text) => <p>{ text }</p>
                    case None => NodeSeq.Empty
                  }
                }
                { dimensions.map(renderQuestion) }
                <p>
                  <input type="submit" id="submitButton" value="Submit"/>
                </p>
              </form>
            </div>
          </body>
        </html>
      }.toString()
  }

  def toXML(randomize: Boolean) = {
    <HTMLQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd">
      <HTMLContent> { scala.xml.PCData(html()) }
      </HTMLContent>
      <FrameHeight>{ _iframe_height.toString }</FrameHeight>
    </HTMLQuestion>
  }
}
