package edu.umass.cs.automan.adapters.MTurk.question

import java.security.MessageDigest

import com.amazonaws.mturk.requester.Assignment
import edu.umass.cs.automan.adapters.MTurk.AutomanHIT
import edu.umass.cs.automan.core.answer.FreeTextAnswer
import edu.umass.cs.automan.core.strategy.PictureClause
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}
import edu.umass.cs.automan.core.question.FreeTextDistributionQuestion
import edu.umass.cs.automan.core.scheduler.Thunk
import org.apache.commons.codec.binary.Hex

import scala.xml.XML

class MTFreeTextDistributionQuestion extends FreeTextDistributionQuestion with MTurkQuestion {
  protected var _allow_empty: Boolean = false
  protected var _before_filter: Symbol => Symbol = (s) => s
  protected var _num_possibilities: BigInt = 1000
  protected var _pattern: Option[String] = None

  def answer(a: Assignment): A = {
    val ans = new FreeTextAnswer(None, a.getWorkerId, _before_filter(answerFromXML(XML.loadString(a.getAnswer))))
    ans.accept_time = a.getAcceptTime
    ans.submit_time = a.getSubmitTime
    ans
  }
  def answerFromXML(x: scala.xml.Node) : Symbol = {
    // There should only be a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //    </Answer>
    Utilities.DebugLog("MTFreeTextDistributionQuestion: fromXML:\n" + x.toString,LogLevel.INFO,LogType.ADAPTER,id)

    Symbol((x \\ "Answer" \\ "SelectionIdentifier").text)
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
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString.getBytes)))
  }
  def num_possibilities: BigInt = _num_possibilities
  def num_possibilities_=(n: BigInt) { _num_possibilities = n }
  def pattern: String = _pattern match { case Some(p) => p; case None => ".*" }
  def pattern_=(p: String) {
    PictureClause(p, _allow_empty) match {
      case (regex, count) => {
        _pattern = Some(regex)
        // the following odd calculation exists to prevent overflow
        // in MonteCarlo simulator; 1/1000 are sufficiently low odds
        _num_possibilities = if (count > 1000) 1000 else count
      }
    }
  }
  def toXML(randomize: Boolean) = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
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
            case Some(x) => <FormattedContent>{ scala.xml.PCData(x.toString()) }</FormattedContent>
            case None => <Text>{ text }</Text>
          }
          }
        </QuestionContent>
        <AnswerSpecification>
          {
          _pattern match {
            case Some(p) => {
              <FreeTextAnswer>
                <Constraints>
                  <AnswerFormatRegex regex={ p } errorText={ pattern_error_text } />
                </Constraints>
              </FreeTextAnswer>
            }
            case None => {
                <FreeTextAnswer />
            }
          }
          }
        </AnswerSpecification>
      </Question>
    </QuestionForm>
  }
}
