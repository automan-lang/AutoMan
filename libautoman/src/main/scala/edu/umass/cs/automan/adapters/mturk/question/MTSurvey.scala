package edu.umass.cs.automan.adapters.mturk.question

import java.security.MessageDigest
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.answer.{Outcome, VariantOutcome}
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.{Question, Survey}
import edu.umass.cs.automan.adapters.mturk.util.XML
import edu.umass.cs.automan.core.info.QuestionType
import org.apache.commons.codec.binary.Hex

import scala.collection.mutable
import scala.xml.{Node, NodeSeq}

class MTSurvey(sandbox: Boolean) extends Survey with MTurkQuestion {

  override def description: String = _description match { case Some(d) => d; case None => this.title }
//  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }
  override def group_id: String = title

  private val _iframe_height = 450

  private val _action = if (sandbox) {
    "https://workersandbox.mturk.com/mturk/externalSubmit"
  } else {
    "https://www.mturk.com/mturk/externalSubmit"
  }
  private val _layout: Option[scala.xml.Node] = None

  override protected[mturk] def fromXML(x: Node): A = {
    var toRet: scala.collection.mutable.Set[(String,Question#A)] = mutable.Set[(String,Question#A)]()

    for(q <- question_list.map(_.question)){
      var id: String = ""
      q match {
        case vq: MTVariantQuestion => id = vq.newQ.id.toString
        case _ => id = q.id.toString
      }
      val a = XML.surveyAnswerFilter(x, id) // gives answer. ID should be question ID
      val ans = q.asInstanceOf[MTurkQuestion].fromXML(a)
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
       |  shuffleQuestions();
       |  shuffleOptions(${generateQIDs()});
       |}
       |
       |function shuffleQuestions() {
       |  var form = document.getElementById('mturk_form');
       |  var questions = document.getElementsByClassName('question');
       |  for (var i = questions.length; i != 0; i--) {
       |    form.appendChild(questions[Math.random() * i | 0]);
       |  }
       |  var submit = document.getElementById('submitButtonP');
       |  form.appendChild(submit);
       |}
       |
       |function shuffleOptions(idArr) {
       |  if (idArr.length != 0) {
       |    for (let i = 0; i != idArr.length; i++) {
       |      shuffleOptionChildren(idArr[i]);
       |    }
       |  }
       |}
       |
       |function shuffleOptionChildren(id) {
       |  var question = document.getElementById(id);
       |  var opts = document.getElementById('opts_' + id);
       |  for (var i = opts.children.length; i != 0; i--) {
       |    opts.appendChild(opts.children[Math.random() * i | 0]);
       |  }
       |}
    """.stripMargin
  }
//  opts.forEach(function(optSet) {
//    for (var i = optSet.children.length; i != 0; i--) {
//      opts.appendChild(opts.children[Math.random() * i | 0]);
//    }
//  });
//shuffleOptions(${generateQIDs()});
//  var question = document.getElementById(id)
//  var opts = question.querySelector('opts');
//  insertOptions();
//  private def addNode(afterTerm: String, to: Node, newNode: Node): Node = {
//    (to \ afterTerm) match {
//      case Node(str, data, node) => {
//        new Node(str, data, node ++ newNode)
//      }
//    }
//  }

//  function shuffleChildren() {
//    console.log('shuffling kids');
//    var opts = document.querySelectorAll('opts');
//    console.log('opt length ' + opts.length);
//    for (var i = 0; i != opts.length; i++){
//      console.log(i);
//      var curOpts = opts[i];
//      console.log('option');
//      console.log('option: ' + curOpts);
//      for (var j = curOpts.children.length; j != 0; j--) {
//        curOpts.appendChild(curOpts.children[Math.random() * j | 0]);
//      }
//    }
//  }

  private def initQ(q: Outcome[_]): Unit = {
    q.question.getQuestionType match {
      case QuestionType.VariantQuestion =>
        q.question.asInstanceOf[MTVariantQuestion].initQuestion(sandbox)
      case _ => ()
    }}

  override protected[mturk] def toXML(randomize: Boolean): Node = {
//    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
//      { XMLBody(randomize) }
//    </QuestionForm>
    // todo match on q type

    _question_list.foreach(initQ)
    <HTMLQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd">
      <HTMLContent>
        { scala.xml.PCData(html(randomize)) }
      </HTMLContent>
      <FrameHeight>{ _iframe_height.toString }</FrameHeight>
    </HTMLQuestion>
  }

  private def html(randomize: Boolean): String = {
    val toRet = String.format("<!DOCTYPE html>%n") + {
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
          <form name='mturk_form' method='post' id='mturk_form' action={_action}>
            <input type="hidden" value={id.toString} name="question_id" id="question_id"/>
            <input type="hidden" value="" name="assignmentId" id="assignmentId"/>
            { toQuestionXML(randomize) }
            <p id='submitButtonP'><input type='submit' id='submitButton' value='Submit'/></p>
          </form>
        </body>
      </html>
    }
    println(toRet)
    toRet
  }

  // Helper method to construct a JS array of all the Question IDs of this Survey
  private def generateQIDs(): String = {
    val toRet: StringBuilder = new StringBuilder("[")

    if(_question_list.size == 1) { // todo make less hacky
      val q = _question_list.head // todo make more efficient
      q match {
        case VariantOutcome(_, _) =>
          toRet.append("'" + q.question.asInstanceOf[MTVariantQuestion]._newQ.id.toString)
      }
    } else {
      for (i <- 0 until _question_list.size - 1) { // append first elems with commas
        val q = _question_list(i) // todo make more efficient
        q match {
          case VariantOutcome(_, _) =>
            toRet.append("'" + q.question.asInstanceOf[MTVariantQuestion]._newQ.id.toString)
          case _ =>
            toRet.append("'" + q.question.id.toString + "','")
        }
      }

      // append last q
      val finalQ = _question_list.last
      finalQ match {
        case VariantOutcome(_, _) =>
          toRet.append(finalQ.question.asInstanceOf[MTVariantQuestion]._newQ.id.toString) // append last elem without a comma
        case _ => toRet.append(finalQ.question.id.toString)
      }
    }
    toRet.append("']")
    toRet.toString()
  }


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
    for(o <- _question_list) {
      val q: Question = o.question // add VariantQ case
      q match {
        case vq: MTVariantQuestion => {
          val ans = vq.prettyPrintAnswer(ansMap(vq.newQ.id.toString).asInstanceOf[vq.A])
          ansString.append(ans + "\n")
        }
        case _ => {
          val ans: Question#A = ansMap(q.id.toString)
          val ppans = q.prettyPrintAnswer(ans.asInstanceOf[q.A])
          ansString.append(ppans + "\n") //A: Set[(String,Question#A)]
        }
      }
    }
    ansString.toString()
  }

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def toQuestionXML(randomize: Boolean): Seq[Node] = {
    val node = _question_list.map(_.question.asInstanceOf[MTurkQuestion].toSurveyXML(randomize))
    node
  }

  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
    throw new Exception("Why are you calling this?")
  }
}
