package org.automanlang.adapters.mturk.question

import java.util.{Date, UUID}

import scala.util.control.Breaks._
import org.automanlang.adapters.mturk.mock.SurveyMockResponse
import org.automanlang.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import org.automanlang.core.logging._
import org.automanlang.core.question.SurveyQuestion
import org.automanlang.core.util.Utilities
import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex

import scala.xml.{Node, NodeSeq}

class MTSurveyQuestion extends SurveyQuestion with MTurkQuestion {
  type QuestionOptionType = MTQuestionOption
  override type A = SurveyQuestion#A

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def randomized_options: List[QuestionOptionType] = Utilities.randomPermute(options)
  override def description: String = _description match { case Some(d) => d; case None => this.title }
  //  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }
  override def group_id: String = title


  // method to create the text using the grammar
  def parseGrammar(t: String) : (String, scala.collection.mutable.Map[String,String]) = {

    // mutable map to keep track of grammar bindings
    val bindings = collection.mutable.Map[String,String]()

    val r = scala.util.Random

    val lengthOfString = t.length()

    var preString = t

    var postString  = ""

    breakable {

      while (true) {
        var start = preString.indexOf("[")
        var end = preString.indexOf("]")

        // if there are no more brackets or they aren't in the right order, break the loop
        if (start == -1 || end == -1 || end < start) {
          break()
        }

        // get the variable
        val v = preString.substring(start + 1, end)
        // get the binding, if it exists
        val b = bindings get v

        var finalWord = ""

        // see if it existed of not
        b match {
          case None => {
            val listOfPossibilitiesOption = _grammar get v
            var listOfPossibilities = List[String]()

            listOfPossibilitiesOption match {
              case None => {
                throw new Exception("Invalid grammar!")
              }
              case Some(l) => listOfPossibilities = l
            }

            val length = listOfPossibilities.length


            // get the length of the list that this variable is attached to
            val randomNum = r.nextInt(length)

            // get the random binding
            finalWord = listOfPossibilities(randomNum)

            // add the binding to the bindings map
            bindings(v) = finalWord

          }
          case Some(n) => finalWord = n
        }

        // build up the postString using the binding
        postString = postString + preString.substring(0,start) + finalWord

        // push the index of the preString, if it's not the entire string

        if (end + 1 < lengthOfString) {
          preString = preString.substring(end + 1)
        }

      }

    }
    // update the postString

    (postString, bindings)

  }

  // private API
  _minimum_spawn_policy = MTurkMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID) : SurveyMockResponse = {
    SurveyMockResponse(question_id, response_time, a, worker_id)
  }
  override protected[mturk] def fromXML(x: scala.xml.Node) : A = { // CheckboxQuestion#A
    // There may be MULTIPLE answers here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //      <QuestionIdentifier>721be34c-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>spongebob</SelectionIdentifier>
    //    </Answer>
    DebugLog("MTCheckboxQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)

    //(x \\ "Answer" \\ "SelectionIdentifier").map{si => Symbol(si.text.drop(1))}.toSet

    val answers = (x \\ "Answer").toList

    // For each item in the answers array, get that item from the questions array and use its fromXML method
    var i = 0

    val l : List[Any] = answers.map(a => {
      val formattedA = this.questions(i).asInstanceOf[MTurkQuestion].fromXML(a)
      i += 1
      formattedA
    })

    l

  }
  // TODO: random checkbox fill
  override protected[mturk] def toXML(randomize: Boolean): scala.xml.Node = {

    val a = parseGrammar(text)
    val newText = a._1
    val bindings = a._2

    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      <Overview>
        <Text>{ this.text }</Text>
      </Overview>
      { this.questions.map(q => q.asInstanceOf[MTurkQuestion].toQuestionXML(randomize)) }
    </QuestionForm>
  }



  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def toQuestionXML(randomize: Boolean): Seq[Node] = {
    <Question>
      <QuestionIdentifier>{ if (randomize) id_string else "" }</QuestionIdentifier>
      <QuestionContent>
        {
        _image_url match {
          case Some(url) => {
            <Binary>
              <MimeType>
                <Type>image</Type>
                <SubType>png</SubType>
              </MimeType>
              <DataURL>{ url }</DataURL>
              <AltText>{ image_alt_text }</AltText>
            </Binary>
          }
          case None => {}
        }
        }
        {
        // if formatted content is specified, use that instead of text field
        _formatted_content match {
          case Some(x) => <FormattedContent>{ scala.xml.PCData(x.toString) }</FormattedContent>
          case None => <Text>{ _text }</Text>
        }
        }
      </QuestionContent>
      <AnswerSpecification>
        <SelectionAnswer>
          <StyleSuggestion>checkbox</StyleSuggestion>
          <Selections>{ if(randomize) randomized_options.map { _.toXML(false) } else options.map { _.toXML(false) } }</Selections>
        </SelectionAnswer>
      </AnswerSpecification>
    </Question>
  }


  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
    <div id={id.toString} class="question">
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
      <div id={s"opts_${id.toString}"}>
        {options.map(_.toSurveyXML(id))}
      </div>
    </div>
  }
}