package edu.umass.cs.automan.adapters.mturk.question

import java.io.{File, PrintWriter}
import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.answer.Outcome
import edu.umass.cs.automan.core.logging.{DebugLog, LogLevelDebug, LogType}
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.{Question, Survey}
import edu.umass.cs.automan.adapters.mturk.util.XML
import edu.umass.cs.automan.core.info.QuestionType
import org.apache.commons.codec.binary.Hex

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.xml.{Elem, Node, NodeSeq}

class MTSurvey(sandbox: Boolean) extends Survey with MTurkQuestion {

  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }

  private var _iframe_height = 450

  private val _action = if (sandbox) {
    "https://workersandbox.mturk.com/mturk/externalSubmit"
  } else {
    "https://www.mturk.com/mturk/externalSubmit"
  }
  private var _layout: Option[scala.xml.Node] = None

  override protected[mturk] def fromXML(x: Node): A = { // Set[(String,Question#A)]
//    DebugLog("MTRadioButtonQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)
//
//  ((x \\ "Answer" \\ "SelectionIdentifier").text, )

//    DebugLog("MTSurvey: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)
//    ((x \\ "Answer" \\ "SelectionIdentifier").text,(x \\ "QuestionIdentifier"))
    //val xml = x
//    val writer = new PrintWriter(new File("xml.txt"))
//    writer.write(x.toString())
//    writer.close()

    var toRet: scala.collection.mutable.Set[(String,Question#A)] = mutable.Set[(String,Question#A)]()

    for(q <- question_list.map(_.question)){
      //var id = ""
      var id: String = ""
      q match {
        case vq: MTVariantQuestion => {
          id = vq.newQ.id.toString()
        }
        case _ => {
          id = q.id.toString()
        }
      }
      //val id = q.id.toString
      //(x \\ "Answer" \\ "QuestionIdentifier").filter { n => n.text == id} // should pull the answer that matches the UUID
      val a = XML.surveyAnswerFilter(x, id) // gives answer
      val ans = q.asInstanceOf[MTurkQuestion].fromXML(a)
      //val n = ((x \\ "SelectionIdentifier").text, q)
      val ansTup: (String, Question#A) = (id, ans.asInstanceOf[Question#A])
      toRet = toRet + ansTup
    }
    toRet.toSet // looks good

    // map from QID to SID
    // get question ID and call correct fromXML
    // return Set[(selection, ...
  }

  def jsFunctions : String = {
    s"""
       |function getAssignmentID() {
       |  return location.search.match(/assignmentId=([_0-9a-zA-Z]+)/)[1];
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
       |  insertOptions();
       |}
       |
       |function shuffle(array) {
       |  if (array.length != 0) {
       |    for (let i = array.length - 1; i != 0; i--) {
       |      const j = Math.floor(Math.random() * (i + 1));
       |      [array[i], array[j]] = [array[j], array[i]];
       |    }
       |  }
       |}
    """.stripMargin
  }

//  private def addNode(afterTerm: String, to: Node, newNode: Node): Node = {
//    (to \ afterTerm) match {
//      case Node(str, data, node) => {
//        new Node(str, data, node ++ newNode)
//      }
//    }
//  }

  override protected[mturk] def toXML(randomize: Boolean): Node = {
//    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
//      { XMLBody(randomize) }
//    </QuestionForm>
    //<![CDATA[
    //      <!DOCTYPE html>
    <HTMLQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd">
      <HTMLContent><![CDATA[
          <!DOCTYPE html>
          <head>
            <title>Please fill out this survey</title>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <script>{ jsFunctions }</script>

            {
            _layout match {
              case Some(layout) => layout
              case None => NodeSeq.Empty
            }
            }
            <script src="https://assets.crowd.aws/crowd-html-elements.js"></script>
          </head>
          <body onload="startup()">
            <div id="wrapper">
              <div id="hit_content">
                <crowd-form name="mturk_form" method="post" id="mturk_form" action={_action}>
                  {
                  var toInsert: NodeSeq = <div></div>
                  for (q <- _question_list) {
                    toInsert = toInsert ++ generateQuestionText(q.question)
                    toInsert = toInsert ++ generateQuestionOpts(q.question)
                  }
                  //XMLBody(randomize)
                  }
                  <p>
                    <input type="submit" id="submitButton" value="Submit"/>
                  </p>
                  </crowd-form>
                </div>
              </div>
            </body>
        </html>

      ]]></HTMLContent>
      <FrameHeight>{ _iframe_height.toString }</FrameHeight>
    </HTMLQuestion>
  }

  private def generateQuestionText(question: Question): Node = {
    <div>question.text</div>
  }

  private def generateQuestionOpts(question: Question): NodeSeq = {
    val qName = s"option_${question.id}"
    var toRet: NodeSeq = <div></div>

    question.getQuestionType match {
      case QuestionType.VariantQuestion => {
        question.asInstanceOf[MTVariantQuestion].initQuestion()
        generateQuestionOpts(question.asInstanceOf[MTVariantQuestion].getInternalQuestion)
      }
      case QuestionType.RadioButtonQuestion => {
        for (o <- question.asInstanceOf[MTRadioButtonQuestion].options) {
          var toApp = <crowd-radio-button name={qName.toString} id={question.id.toString}>{o.question_text}</crowd-radio-button>
          toRet = toRet ++ toApp
        }
        toRet
      }
      case _ => throw new Error("other question types not implemented")
    }
  }

//  <p>
//    <input type="submit" id="submitButton" value="Submit"/>
//  </p>
  //      val toAdd = {
  //        <p>
  //          <input type="submit" id="submitButton" value="Submit"/>
  //        </p>
  //      }
  //
  //      val bods: NodeSeq = (bod \ "body")
  //        bods.tail ++= toAdd // added submit button to end
  //
  //        val topHTML = (bod \ "html")
  //        topHTML match {
  //          case Node(str, data, node) => topHTML = Node(str, data, bods)
  //          //case Elem(_,_,_,_,child) => topHTML = Elem(_,_,_,_, bods)
  //          //case Elem(_,_,_,child) => topHTML = Elem(_,_,_,bods)
  //        }
  //topHTML.child

  //val toAdd = "</body><p><input type='submit' id='submitButton' value='Submit'/></p>"
  //val bodString = bod.toString() // adding List
  //var bodArr: ArrayBuffer[String] = ArrayBuffer()
  // need to insert after last instance of </body>
  //bodArr ++= bodString.split("</body>")
  //assert(bodArr.length == 2)
  //      bodArr.insert(bodArr.length - 1, toAdd)
  //      val newBod = bodArr.toString()
  //xml.XML.loadString(newBod)

  //          bod = bod ++ {
  //            <p>
  //              <input type="submit" id="submitButton" value="Submit"/>
  //            </p>
  //          }
  //val tl = bod.last
  //      val bodyDiv: NodeSeq = (bod \ "body") // todo combine nodes into one node?
  //      assert(bodyDiv.size == 1)
  //      bodyDiv.head match {
  //
  //      }

  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    val concat = _question_list.foldLeft("")((acc, o: Outcome[_]) => {
      acc + o.question.memo_hash
    })
    val toRet = new String(Hex.encodeHex(md.digest(concat.getBytes)))
    toRet
  }

  override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: Set[(String, Question#A)], worker_id: UUID): MockResponse = ???

  // prints answers grouped by worker
  // TODO apologies for object orientation
  override protected[automan] def prettyPrintAnswer(answer: Set[(String, Question#A)]): String = {
    val ansString: StringBuilder = new StringBuilder()
    val ansMap: Map[String, Question#A] = answer.toMap
    //var ans: Question#A = null
    for(o <- _question_list) {
      val q: Question = o.question // add VariantQ case
      q match {
        case vq: MTVariantQuestion => {
          val ans = vq.prettyPrintAnswer(ansMap(vq.newQ.id.toString).asInstanceOf[vq.A])
          ansString.append(ans + "\n")
//          val ans: Question#A = ansMap(vq.newQ.id.toString) // this is just a question identifier
//          val ppans = q.prettyPrintAnswer(ans.asInstanceOf[q.A])
//          ansString.append(ppans) //A: Set[(String,Question#A)] // so this is also getting the question ID
        }
        case _ => {
          val ans: Question#A = ansMap(q.id.toString)
          val ppans = q.prettyPrintAnswer(ans.asInstanceOf[q.A])
          ansString.append(ppans + "\n") //A: Set[(String,Question#A)]
        }
      }
      //val ans: Question#A = ansMap(q.id.toString)
//      val ppans = q.prettyPrintAnswer(ans.asInstanceOf[q.A])
//      println(s"printing ${q.id} answer: ${ppans}")
      //ansString.append(ppans) //A: Set[(String,Question#A)]
    }
    ansString.toString()

    //    var toRet: StringBuilder = new StringBuilder
//    for(a <- answer){
//      //val (_,(question_id,ans)) = a
//      val (question_id,ans) = a
//      toRet ++= s"${question_id}: ${ans}\n"
//    }
//    val toRetStr = toRet.toString()
//    toRetStr
////    val (_,(question_id,ans)) = answer
////    s"${question_id}: ${ans}"
  }

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def XMLBody(randomize: Boolean): Seq[Node] = {
    val node = _question_list.map(_.question.asInstanceOf[MTurkQuestion].toSurveyXML(randomize))
    node
  }

  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
    throw new Exception("Why are you calling this?")
  }
}
