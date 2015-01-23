package edu.umass.cs.automan.adapters.MTurk.question

import edu.umass.cs.automan.adapters.MTurk.AutomanHIT
import edu.umass.cs.automan.core.scheduler.Thunk
import com.amazonaws.mturk.requester.Assignment
import xml.XML
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import edu.umass.cs.automan.core.question.FreeTextQuestion
import edu.umass.cs.automan.core.answer.FreeTextAnswer
import edu.umass.cs.automan.core.strategy.PictureClause
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

class MTFreeTextQuestion extends FreeTextQuestion with MTurkQuestion {

  protected var _before_filter: Symbol => Symbol = (s) => s
  protected var _options = List[MTQuestionOption]()
  protected var _internal_pattern: Option[String] = None

  def answer(a: Assignment): A = {
    val ans = new FreeTextAnswer(None, a.getWorkerId, _before_filter(answerFromXML(XML.loadString(a.getAnswer))))
    ans.accept_time = a.getAcceptTime
    ans.submit_time = a.getSubmitTime
    ans
  }
  def build_hit(ts: List[Thunk[_]]) : AutomanHIT = {
    val x = toXML(randomize = false)
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
  def options: List[MTQuestionOption] = _options
  def options_=(os: List[MTQuestionOption]) { _options = os }
  def answerFromXML(x: scala.xml.Node) = {
    Utilities.DebugLog("MTFreeTextQuestion: fromXML:\n" + x.toString,LogLevel.INFO,LogType.ADAPTER,id)

    Symbol((x \\ "Answer" \ "FreeText").text)
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
  def allow_empty_pattern_=(ae: Boolean) { _allow_empty = ae }
  def allow_empty_pattern: Boolean = _allow_empty
  def before_filter_=(f: Symbol => Symbol) { _before_filter = f }
  def before_filter: Symbol => Symbol = _before_filter
}