package org.automanlang.adapters.mturk.question

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.automanlang.core.grammar.JsonEncoder._
import org.apache.commons.codec.binary.Hex
import org.automanlang.core.grammar.Expand.expand
import org.automanlang.core.grammar.{QuestionProduction, RadioQuestionProduction}
import org.automanlang.core.grammar.Rank.{Grammar, generateBases}
import org.automanlang.core.info.QuestionType
import org.automanlang.core.info.QuestionType.QuestionType
import org.automanlang.core.question.{FakeSurvey, GrammarSurvey}

import java.security.MessageDigest
import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.xml.Node

class MTGrammarSurvey extends GrammarSurvey with MTurkQuestion {
  override protected var _grammar: List[Grammar] = _
  override protected var _types: List[QuestionType] = _
  override protected var _variant: List[Int] = _
  override protected var _depth: Int = _

  var initialized: Boolean = false
  var expandedGrammars: List[Grammar] = _
  var concreteQuestions: List[MTurkQuestion] = _
  var options_ids: Array[List[String]] = _

  override type Q = MTurkQuestion

  def initQuestion(): List[MTurkQuestion] = {
    val prod = questionProduction()

    options_ids = prod.map(_=> List[String]()).toArray
    prod.zipWithIndex.map{ case (p, i) =>
      val (body, opts) = p.toQuestionText(variant(i), depth)
      val bodyText: String = body

      p.questionType match {
        case QuestionType.EstimationQuestion => {
          val newQ = new MTEstimationQuestion()
          newQ.text = bodyText
          newQ
        }
        case QuestionType.CheckboxQuestion => {
          val newQ = new MTCheckboxQuestion()
          newQ.text = bodyText
          val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
          newQ.options = options
          options_ids(i) = options.map(o => newQ.id_string + "-" + o.question_id.toString())
          newQ
        }
        case QuestionType.RadioButtonQuestion => {
          val newQ = new MTRadioButtonQuestion()
          newQ.text = bodyText
          val options: List[MTQuestionOption] = opts.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
          newQ.options = options
          options_ids(i) = options.map(o => newQ.id_string + "-" + o.question_id.toString())
          newQ
        }
      }
    }
  }


  override def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(id.toString.getBytes))
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
  override protected[mturk] def fromXML(x: Node): A = ???

  override protected[mturk] def fromHTML(x: Node): A = {
    val map = collection.mutable.Map[UUID, Q]()
    for (q <- concreteQuestions) {
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
    for (q <- concreteQuestions) {
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
  override protected[mturk] def toQuestionXML(randomize: Boolean): Seq[Node] = ???

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
              <div id="header-container">
                <h1>${title}</h1><p>${text}</p>
              </div>
              <crowd-form>
                <div id="container">
                ${toQuestionHTML(randomize)}
                </div>
              </crowd-form>
              ${randomizeScript(randomize)}
            </body>
          </html>
        ]]>
      </HTMLContent>
      <FrameHeight>0</FrameHeight>
    </HTMLQuestion>
    """
  }

  def generateMadLibsChoices(): String = {
    val choices = words_candidates.map( candidate =>{
      val choices = candidate._2.map(x=>s""""$x"""").mkString(",")
      s"[$choices]"
    }).mkString(",")
    s"[${choices}]"
  }

  def generateMadLibsAssignment(): String = {
    words_candidates.zipWithIndex.map{ case (candidate, i) =>
      s"let ${candidate._1} = choices[${i}][assignment[${i}]];"
    }.mkString("\n")
  }

  // https://stackoverflow.com/questions/23215162/assignmentid-not-visible-in-mturk-accept-url
  //
  def randomizeScript(randomize: Boolean): String = {
    s"""
       |<script>
       |const shuffleNodes = () => {
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
       |  const bindHelper = (expr, g, assignment, generatingScope, generatedNames) => {
       |    let index = generatingScope.size;
       |    let soFarScope = generatingScope;
       |    let soFarNames = generatedNames;
       |
       |    // console.log(expr);
       |    switch (expr["type"]) {
       |      case "Ref":
       |        return bindHelper(g[expr["nt"]["fullname"]], g, assignment, generatingScope, new Set(soFarNames).add(expr["nt"]["text"]));
       |      case "OptionProduction":
       |        return bindHelper(expr["text"], g, assignment, generatingScope, soFarNames);
       |      case "Terminal":
       |      case "Function":
       |        return {soFarScope, soFarNames};
       |      case "Sequence":
       |        expr["sentence"].forEach(e => {
       |          ({soFarScope, soFarNames} = bindHelper(e, g, assignment, soFarScope, soFarNames));
       |        });
       |        return {soFarScope, soFarNames};
       |      case "Choice":
       |        let choices = expr["choices"];
       |        soFarScope.set(index, choices[assignment[index]]);
       |        choices.forEach(e => {
       |          ({soFarScope, soFarNames} = bindHelper(e, g, assignment, soFarScope, soFarNames))
       |        })
       |        return {soFarScope, soFarNames};
       |      case "Binding":
       |        if(!soFarNames.has(expr["nt"]["text"])) {
       |          return bindHelper(g[expr["nt"]["fullname"]], g, assignment, generatingScope, new Set(soFarNames).add(expr["nt"]["text"]))
       |        } else {
       |          return {soFarScope, soFarNames}
       |        }
       |      default:
       |        break;
       |    }
       |  }
       |
       |  const bind = (g, assignment) => {
       |    let {soFarScope, } = bindHelper(g["Start"], g, assignment, new Map(), new Set());
       |    return soFarScope
       |  }
       |
       |  const firstRender = (expr, g, scope, generatingBod, generatingOpts, doAppend, index, boundVars) => {
       |    let bodSoFar = generatingBod
       |    let optsSoFar = generatingOpts
       |    let boundVarsSoFar = boundVars
       |    let position = index
       |
       |    switch (expr["type"]) {
       |      case "Ref":
       |        return firstRender(g[expr["nt"]["fullname"]], g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
       |      case "Terminal":
       |      case "Function":
       |        if (doAppend) { bodSoFar = bodSoFar.concat(expr); }  // append if we're on the right branch
       |        return {bodSoFar, optsSoFar, boundVarsSoFar, position};
       |      case "Sequence":
       |        expr["sentence"].forEach(e => {
       |          ({bodSoFar, optsSoFar, boundVarsSoFar, position} = firstRender(e, g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar));
       |        });
       |        return {bodSoFar, optsSoFar, boundVarsSoFar, position};
       |      case "Choice":
       |        position += 1;
       |        let choices = expr["choices"];
       |        choices.forEach(e => {
       |          let doApp = false;
       |          // TODO: assuming index comparison works (it will be our simplified version of hashing)
       |          if (doAppend && scope.get(position)["index"] === e["index"]) {
       |            doApp = true;
       |          }
       |          ({bodSoFar, optsSoFar, boundVarsSoFar, position} = firstRender(e, g, scope, bodSoFar, optsSoFar, doApp, position, boundVarsSoFar));
       |        })
       |        return {bodSoFar, optsSoFar, boundVarsSoFar, position};
       |      case "OptionProduction":
       |        // here we reuse firstRender instead of reimplementing Emmie's renderHelper
       |        // the core idea is to treat the options as body, and reuse the function logic to construct a body
       |        let {bodSoFar: newOpt, boundVarsSoFar: newBound, position: newPos } = firstRender(expr["text"], g, scope, [], [], doAppend, position, boundVarsSoFar);
       |        return {bodSoFar, optsSoFar: optsSoFar.concat([newOpt]), boundVarsSoFar: newBound, position: newPos};
       |      case "Binding":
       |        let startArr = Array.from(bodSoFar);
       |
       |        if(!boundVarsSoFar.has(expr["nt"]["text"])) {
       |          ({bodSoFar, optsSoFar, boundVarsSoFar, position} = firstRender(g[expr["nt"]["fullname"]], g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar))
       |          let added = bodSoFar.filter(x => !startArr.includes(x));  // difference: figure out what was added
       |
       |          let toAdd = added.map(x => x["value"]).join("");
       |          boundVarsSoFar = new Map(boundVarsSoFar).set(expr["nt"]["text"], toAdd);
       |        } else {  // if have seen, look up binding and add to instance
       |          bodSoFar = bodSoFar.concat(JSON.parse(`{"type" : "Terminal","value" : "$${boundVarsSoFar.get(expr["nt"]["text"])}"}`))
       |        }
       |        return {bodSoFar, optsSoFar, boundVarsSoFar, position};
       |      default:
       |        console.log("ERROR: UNKNOWN TYPE");
       |        break;
       |    }
       |  }
       |
       |  const secondRenderHelper = (instArr, bingdingMap) => {
       |    let instance = [];
       |    instArr.forEach(e => {
       |      switch (e["type"]) {
       |        case "Terminal":
       |          instance.push(e["value"]);
       |          break;
       |        case "Function":
       |          let text = e["fun"][bingdingMap.get(e["param"]["text"])];
       |          if (e["capitalize"]) {
       |            text = text.charAt(0).toUpperCase() + text.slice(1)
       |          }
       |          instance.push(text);
       |          break;
       |        default:
       |          break;
       |      }
       |    })
       |    return instance.join("");
       |  }
       |
       |  const secondRender = (bodArr, optsArr, bingdingMap) => {
       |    return {
       |      bod: secondRenderHelper(bodArr, bingdingMap),
       |      opts: optsArr.map(o => secondRenderHelper(o, bingdingMap))
       |    }
       |  }
       |
       |  const renderInstance = (scope, grammar) => {
       |    let {bodSoFar, optsSoFar, boundVarsSoFar} = firstRender(grammar["Start"], grammar, scope, Array(), Array(), true, -1, new Map())
       |    // By now, all grammar should be left with only terminals and functions, we should secondRender to concatenate them and apply functions
       |    return secondRender(bodSoFar, optsSoFar, boundVarsSoFar);
       |  }
       |
       |  let seed = turkGetParam('workerId');
       |  console.log("[DEBUG] workerId" + seed);
       |
       |  // From seedrandom library
       |  let myrng = new Math.seedrandom(seed);
       |
       |  console.log("[DEBUG] Start grammar rendering");
       |  const unrank = (variant, bases) => {
       |    return bases.map((base,index) => {
       |      let prod = 1;
       |      for (let i = 0; i < bases.length-index-1; i++) {
       |        prod *= bases[index+1+i];
       |      }
       |      let quotient = Math.floor(variant / prod);
       |      // assignment for current index
       |      return quotient % base;
       |    })
       |  }
       |
       |  // here scala generate choices (nested array of target phrases)
       |  const grammarText = String.raw`${expandedGrammars.asJson}`;
       |  const basesText = String.raw`${expandedGrammars.map(g => generateBases(g)).asJson}`;
       |  const idsText = String.raw`${concreteQuestions.map(q => q.id).asJson}`;
       |  const optIdsText = String.raw`${options_ids.asJson}`;
       |
       |  const grammars = JSON.parse(grammarText);
       |  const bases = JSON.parse(basesText);
       |  const ids = JSON.parse(idsText);
       |  const opt_ids = JSON.parse(optIdsText);
       |
       |  grammars.forEach((grammar,i) => {
       |    let base = bases[i];
       |    let max = base.reduce( (a,b) => a * b );
       |    let assignment = unrank(Math.floor(myrng() * max), base);
       |    console.log(`[DEBUG] grammar assignment: $${assignment}`);
       |
       |    let scope = bind(grammar, assignment);
       |    console.log("[DEBUG] grammar scope:", scope);
       |
       |    let {bod, opts} = renderInstance(scope, grammar);
       |    console.log("[DEBUG] body:", bod);
       |    console.log("[DEBUG] opts:", opts);
       |    document.getElementById(ids[i]).querySelector(".QuestionContent p").innerText = bod;
       |    opt_ids[i].forEach((o_id,j) => {
       |      document.getElementById(o_id).parentElement.querySelector("label").innerText = opts[j];
       |    })
       |  });
       |
       |  // insert into DOM
       |  console.log("[DEBUG] Start shuffling elements");
       |  const list = document.getElementById("container");
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
    if (!initialized) {
      concreteQuestions = initQuestion()
      expandedGrammars = grammars.map(g => expand(g, depth))
    }

    concreteQuestions.map(
      _.toQuestionHTML(randomize)
    ).mkString
  }
}
