package edu.umass.cs.automan.adapters.MTurk.question

import edu.umass.cs.automan.core.answer.RadioButtonAnswer
import edu.umass.cs.automan.adapters.MTurk.{AutomanHIT, MTurkAdapter}
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import edu.umass.cs.automan.core.question.{QuestionOption, RadioButtonQuestion}
import edu.umass.cs.automan.core.scheduler.Thunk
import com.amazonaws.mturk.requester.Assignment
import xml.XML
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

class MTRadioButtonQuestion extends RadioButtonQuestion with MTurkQuestion {
  type QO = MTQuestionOption
  protected var _options = List[QO]()

  def answer(a: Assignment): A = {
    val ans = new RadioButtonAnswer(None, a.getWorkerId, answerFromXML(XML.loadString(a.getAnswer)))
    ans.accept_time = a.getAcceptTime
    ans.submit_time = a.getSubmitTime
    ans
  }
  def build_hit(ts: List[Thunk[_]]) : AutomanHIT = {
    val x = toXML(randomize = !_dont_randomize_options)
    val h = AutomanHIT { a =>
      a.hit_type_id = _hit_type_id
      a.title = title
      a.description = _description
      a.keywords = _keywords
      a.question_xml = x
      a.assignmentDurationInSeconds = _worker_timeout_in_s
      a.lifetimeInSeconds = question_timeout_in_s
      a.maxAssignments = ts.size
      a.cost = ts.head.cost
      a.id = id
    }
    Utilities.DebugLog("Posting XML:\n" + x,LogLevel.INFO,LogType.ADAPTER,id)
    hits = h :: hits
    hit_thunk_map += (h -> ts)
    h
  }
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  def options: List[QO] = _options
  def options_=(os: List[QO]) { _options = os }
  def answerFromXML(x: scala.xml.Node) : Symbol = {
    // There should only be a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //    </Answer>
    Utilities.DebugLog("MTRadioButtonQuestion: fromXML:\n" + x.toString,LogLevel.INFO,LogType.ADAPTER,id)

    Symbol((x \\ "Answer" \\ "SelectionIdentifier").text)
  }
  // TODO: random checkbox fill
  def toXML(randomize: Boolean) : scala.xml.Node = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      <Question>
        <QuestionIdentifier>{ if (randomize) id_string else "" }</QuestionIdentifier>
        <IsRequired>true</IsRequired>
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
              case Some(x) => <FormattedContent>{ scala.xml.PCData(x.toString()) }</FormattedContent>
              case None => <Text>{ text }</Text>
            }
          }
        </QuestionContent>
        <AnswerSpecification>
          <SelectionAnswer>
            <StyleSuggestion>radiobutton</StyleSuggestion>
            <Selections>{ if(randomize) randomized_options.map { _.toXML } else options.map { _.toXML } }</Selections>
          </SelectionAnswer>
        </AnswerSpecification>
      </Question>
    </QuestionForm>
  }
  override def randomized_options: List[QO] = Utilities.randomPermute(options)
}