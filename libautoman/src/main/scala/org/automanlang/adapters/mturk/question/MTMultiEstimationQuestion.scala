package org.automanlang.adapters.mturk.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import org.automanlang.adapters.mturk.mock.MultiEstimationMockResponse
import org.automanlang.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import org.automanlang.core.logging.{DebugLog, LogLevelDebug, LogType}
import org.automanlang.core.question.{Dimension, MultiEstimationQuestion}
import org.apache.commons.codec.binary.Hex

import scala.xml.{Node, NodeSeq}

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
//  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }
  override def group_id: String = title

  // private API
  override def toMockResponse(question_id: UUID, response_time: Date, as: A, worker_id: UUID) : MultiEstimationMockResponse = {
    MultiEstimationMockResponse(dimensions.map(_.id), response_time, as, worker_id)
  }

  def fromXML(x: scala.xml.Node) : A = {
    DebugLog("MTMultiEstimationQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)
    val answer_map: Map[String, String] = (x \\ "Answer").map { a =>
      (a \ "QuestionIdentifier").text -> (a \ "FreeText").text
    }.toMap
    dimensions.map { dim => answer_map(dim.id.toString.drop(1)).toDouble }
  }

  def renderQuestion(dimension: Dimension) : scala.xml.Node = {
    val idname = s"dimension_${ dimension.id.toString.drop(1) }"
    <p>
      <input type="text" class="dimension" id={ idname } name={ dimension.id.toString.drop(1) } />
    </p>
  }

  def jsFunctions : String = {
    """
      |function getAssignmentID() {
      |  return location.search.match(/assignmentId=(\w+)/)[1];
      |}
      |
      |function previewMode() {
      |  var assignment_id = getAssignmentID();
      |  return assignment_id === 'ASSIGNMENT_ID_NOT_AVAILABLE';
      |}
      |
      |function disableSubmitOnPreview() {
      |  if (previewMode()) {
      |    document.getElementById('submitButton').setAttribute('disabled', true);
      |  }
      |}
      |
      |function startup() {
      |  disableSubmitOnPreview();
      |  document.getElementById('assignmentId').value = getAssignmentID();
      |}
    """.stripMargin
  }

  def html() = {
    String.format("<!DOCTYPE html>%n") +
      {
        <html>
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <script>{ jsFunctions }</script>
            {
              _layout match {
                case Some(layout) => layout
                case None => NodeSeq.Empty
              }
            }
          </head>
          <body onload="startup()">
            <div id="wrapper">
              <div id="hit_content">
                <form name="mturk_form" method="post" id="mturk_form" action={_action}>
                  <input type="hidden" value={id.toString} name="question_id" id="question_id"/>
                  <input type="hidden" value="" name="assignmentId" id="assignmentId"/>
                  {
                  _image_url match {
                    case Some(url) => <p><img id="question_image" src={ url }/></p>
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
            </div>
          </body>
        </html>
      }.toString()
  }

  override protected[mturk] def toXML(randomize: Boolean): scala.xml.Node = {
    <HTMLQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd">
      { toQuestionXML(randomize) }
    </HTMLQuestion>
  }

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def toQuestionXML(randomize: Boolean): Seq[Node] = {
    {
        <HTMLContent> { scala.xml.PCData(html()) }
        </HTMLContent>
        <FrameHeight>{ _iframe_height.toString }</FrameHeight>
    }
  } // calling html()

  // TODO this may not be right
  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = toXML(randomize)
}
