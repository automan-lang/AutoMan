package edu.umass.cs.automan.adapters.MTurk.question

import java.security.MessageDigest

import com.amazonaws.mturk.requester.Assignment
import edu.umass.cs.automan.adapters.MTurk.AutomanHIT
import edu.umass.cs.automan.core.answer.CheckboxAnswer
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}
import edu.umass.cs.automan.core.question.CheckboxDistributionQuestion
import edu.umass.cs.automan.core.scheduler.Thunk
import org.apache.commons.codec.binary.Hex
import scala.xml.XML

class MTCheckboxDistributionQuestion extends CheckboxDistributionQuestion with MTurkQuestion {
  override type QO = MTQuestionOption

  def answer(a: Assignment): A = {
    val xml = XML.loadString(a.getAnswer)
    val ans_symb = answerFromXML(xml)
    val ans = new CheckboxAnswer(None, a.getWorkerId, ans_symb)
    ans.accept_time = a.getAcceptTime
    ans.submit_time = a.getSubmitTime
    ans
  }
  def answerFromXML(x: scala.xml.Node) : Set[Symbol] = {
    (x \\ "Answer" \\ "SelectionIdentifier").map{si => Symbol(si.text)}.toSet
  }
  def build_hit(ts: List[Thunk[_]]) : AutomanHIT = {
    val x = toXML(randomize = !_dont_randomize_options)
    val h = AutomanHIT { a =>
      a.hit_type_id = _hit_type_id
      a.title = title
      a.description = text
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
            case Some(x) => <FormattedContent>{ scala.xml.PCData(x.toString) }</FormattedContent>
            case None => <Text>{ text }</Text>
          }
          }
        </QuestionContent>
        <AnswerSpecification>
          <SelectionAnswer>
            <StyleSuggestion>checkbox</StyleSuggestion>
            <Selections>{ if(randomize) randomized_options.map { _.toXML } else options.map { _.toXML } }</Selections>
          </SelectionAnswer>
        </AnswerSpecification>
      </Question>
    </QuestionForm>
  }
  override def randomized_options: List[QO] = Utilities.randomPermute(options)
}
