package org.automanlang.adapters.mturk.question

import org.apache.commons.codec.binary.Hex
import org.automanlang.core.question.FakeSurvey

import java.security.MessageDigest
import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.xml.Node

import io.circe._, io.circe.parser._
import io.circe.syntax._

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

  override def group_id: String = _id.toString

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

  override protected[mturk] def fromHTML(x: Node): A = {
    val map = collection.mutable.Map[UUID, Q]()
    for (q <- questions) {
      map(q.id) = q
    }

    val json = (x \\ "FreeText").text
    val parsed = parse(json).getOrElse(Json.Null)

    // if the answer is not returned in JSON, set to default value
    val ans = collection.mutable.Map[UUID, Any]().withDefault({ _ => "(No Answer)" })

    val cursor: HCursor = parsed.hcursor
    cursor.downArray.keys.get.foreach(k => {
      val id = UUID.fromString(k)
      val ques = map(id)
      ans(id) = ques.fromHTMLJson(parsed.findAllByKey(k).head)
    })

    // resort answer with input question order
    val finalAns = new ListBuffer[Any]()
    for (q <- questions) {
      finalAns += ans(q.id)
    }

    finalAns.toList
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

  override protected[mturk] def toHTML(randomize: Boolean): String = {
    s"""
    <HTMLQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd">
      <HTMLContent>
        <![CDATA[
          <!DOCTYPE html>
            <body>
              <script src="https://cdnjs.cloudflare.com/ajax/libs/seedrandom/3.0.5/seedrandom.min.js"></script>
              <script src="https://assets.crowd.aws/crowd-html-elements.js"></script>
              <h1>${title}</h1>
              <p>${text}</p>
              <crowd-form>
                <div id="container">
                ${toQuestionHTML(randomize)}
                </div>
              </crowd-form>
              ${randomizeScript()}
            </body>
          </html>
        ]]>
      </HTMLContent>
      <FrameHeight>0</FrameHeight>
    </HTMLQuestion>
    """
  }

  // https://stackoverflow.com/questions/23215162/assignmentid-not-visible-in-mturk-accept-url
  //
  def randomizeScript(): String = {
    s"""
       |<script>
       |const shuffleNodes = () => {
       |  console.log("[DEBUG] Start shuffling elements");
       |  const list = document.getElementById("container");
       |
       |  function turkGetParam( name ) {
       |    var regexS = "[\\?&]"+name+"=([^&#]*)";
       |    var regex = new RegExp( regexS );
       |    var tmpURL = window.location.href;
       |    var results = regex.exec( tmpURL );
       |    if( results == null ) {
       |        return "";
       |    } else {
       |        return results[1];
       |    }
       |  }
       |
       |  let seed = turkGetParam('workerId');
       |  console.log("[DEBUG] workerId" + seed);
       |
       |  // the Knuth shuffle, where prng returns [0..1)
       |  const shuffle = (items, prng) => {
       |    let cached = items.slice(0);
       |    let i = cached.length;
       |    let temp, rand;
       |    while (--i > 0) {
       |      rand = Math.floor((i+1) * prng());
       |      temp = cached[rand];
       |      cached[rand] = cached[i];
       |      cached[i] = temp;
       |    }
       |    return cached;
       |  }
       |
       |  // From seedrandom library
       |  let myrng = new Math.seedrandom(seed);
       |
       |  // shuffle questions
       |  let nodes = list.children, i = 0;
       |  nodes = Array.prototype.slice.call(nodes);
       |  nodes = shuffle(nodes, myrng);
       |
       |  // writeback questions
       |  while (i < nodes.length) {
       |    list.appendChild(nodes[i]);
       |    ++i;
       |  }
       |  console.log("[DEBUG] Finish shuffling questions");
       |
       |  // shuffle options within each question, if any
       |  for (let index = 0; index < nodes.length; index++) {
       |    const element = nodes[index];
       |    let childNodes = element.getElementsByClassName("option"),
       |      i = 0;
       |
       |    if (childNodes.length === 0) {
       |      continue;
       |    }
       |
       |    console.log(`[DEBUG] shuffling options for $${element.id}`);
       |    childNodes = Array.prototype.slice.call(childNodes);
       |    childNodes = shuffle(childNodes, myrng);
       |
       |    // writeback
       |    while (i < childNodes.length) {
       |      element.appendChild(childNodes[i]);
       |      ++i;
       |    }
       |    console.log(`[DEBUG] written back options for $${element.id}`);
       |  }
       |
       |  console.log("[DEBUG] Finish shuffling all");
       |}
       |
       |document.addEventListener('DOMContentLoaded', shuffleNodes, false);
       |</script>
       |""".stripMargin
  }

  override protected[mturk] def toQuestionHTML(randomize: Boolean): String = {
    questions.map(
      _.toQuestionHTML(randomize)
    ).mkString
  }
}
