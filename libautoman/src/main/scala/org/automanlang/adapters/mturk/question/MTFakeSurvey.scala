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
       |  // a custom seeded PRNG function generator.
       |  // Taken from https://stackoverflow.com/a/47593316/6604166
       |  const xmur3 = (str) => {
       |    for (var i = 0, h = 1779033703 ^ str.length; i < str.length; i++) {
       |      h = Math.imul(h ^ str.charCodeAt(i), 3432918353);
       |      h = h << 13 | h >>> 19;
       |    } return function () {
       |      h = Math.imul(h ^ (h >>> 16), 2246822507);
       |      h = Math.imul(h ^ (h >>> 13), 3266489909);
       |      return (h ^= h >>> 16) >>> 0;
       |    }
       |  }
       |
       |  const sfc32 = (a, b, c, d) => {
       |    return function () {
       |      a >>>= 0; b >>>= 0; c >>>= 0; d >>>= 0;
       |      var t = (a + b) | 0;
       |      a = b ^ b >>> 9;
       |      b = c + (c << 3) | 0;
       |      c = (c << 21 | c >>> 11);
       |      d = d + 1 | 0;
       |      t = t + d | 0;
       |      c = c + t | 0;
       |      return (t >>> 0) / 4294967296;
       |    }
       |  }
       |
       |  const shuffle = (items, prng) => {
       |    let cached = items.slice(0);
       |    let i = cached.length;
       |    let temp, rand;
       |    while (--i) {
       |      rand = Math.floor(i * prng());
       |      temp = cached[rand];
       |      cached[rand] = cached[i];
       |      cached[i] = temp;
       |    }
       |    return cached;
       |  }
       |
       |  // Create xmur3 state:
       |  const seeder = xmur3(seed);
       |  // Output four 32-bit hashes to provide the seed for sfc32.
       |  const rand = sfc32(seeder(), seeder(), seeder(), seeder());
       |
       |  // shuffle
       |  let nodes = list.children, i = 0;
       |  nodes = Array.prototype.slice.call(nodes);
       |  nodes = shuffle(nodes, rand);
       |
       |  // writeback
       |  while (i < nodes.length) {
       |    list.appendChild(nodes[i]);
       |    ++i;
       |  }
       |  console.log("[DEBUG] Finish shuffling elements")
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
