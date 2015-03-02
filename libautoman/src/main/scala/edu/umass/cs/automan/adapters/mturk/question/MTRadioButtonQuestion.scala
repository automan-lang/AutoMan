package edu.umass.cs.automan.adapters.mturk.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.answer.Answer
import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.{Question, RadioButtonQuestion}
import edu.umass.cs.automan.core.scheduler.{SchedulerState, Scheduler, BackendResult, Thunk}
import com.amazonaws.mturk.requester.{HIT, Assignment}
import edu.umass.cs.automan.core.util.Utilities
import xml.XML
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

class MTRadioButtonQuestion extends RadioButtonQuestion with MTurkQuestion {
  type QO = MTQuestionOption
  type A = Symbol

  override protected var _group_id: String = _
  protected var _options = List[QO]()

  def answer(a: Assignment): BackendResult[A] = {
    new BackendResult[A](
      fromXML(XML.loadString(a.getAnswer)),
      a.getWorkerId,
      a.getAcceptTime.getTime,
      a.getSubmitTime.getTime
    )
  }
  override protected[automan] def getAnswer(scheduler: Scheduler[A]): Answer[A] = ???
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  def options: List[QO] = _options
  def options_=(os: List[QO]) { _options = os }
  def fromXML(x: scala.xml.Node) : A = {
    // There should only be a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //    </Answer>
    DebugLog("MTRadioButtonQuestion: fromXML:\n" + x.toString,LogLevel.INFO,LogType.ADAPTER,id)

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