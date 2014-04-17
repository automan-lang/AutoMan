package edu.umass.cs.automan.adapters.MTurk.question

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex
import edu.umass.cs.automan.adapters.MTurk.{AutomanHIT, MTurkAdapter}
import edu.umass.cs.automan.core.scheduler.Thunk
import com.amazonaws.mturk.requester.Assignment
import xml.XML
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import edu.umass.cs.automan.core.question.{FreeTextQuestion, RadioButtonQuestion}

import edu.umass.cs.automan.core.answer.{FreeTextAnswer, RadioButtonAnswer}
import edu.umass.cs.automan.core.strategy.PictureClause
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

object MTFreeTextQuestion {
  def apply(init: MTFreeTextQuestion => Unit, a: MTurkAdapter) : Future[FreeTextAnswer] = {
    val free_text_question = new MTFreeTextQuestion
    init(free_text_question)
    a.schedule(free_text_question)
  }
}

class MTFreeTextQuestion extends FreeTextQuestion with MTurkQuestion {
  protected var _options = List[MTQuestionOption]()
  protected var _pattern: Option[String] = None
  protected var _internal_pattern: Option[String] = None
  protected var _num_possibilities: BigInt = 1000
  protected var _allow_empty: Boolean = false
  protected var _before_filter: Symbol => Symbol = (s) => s

  def answer(a: Assignment, is_dual: Boolean): FreeTextAnswer = {
    // ignore is_dual because FreeTextQuestions have no question duals
    val answer = fromXML(XML.loadString(a.getAnswer))
    new FreeTextAnswer(None, a.getWorkerId, _before_filter(answer))
  }
  def build_hit(ts: List[Thunk], is_dual: Boolean) : AutomanHIT = {
    // we ignore the "dual" option here
    val x = toXML(false, true)
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
  def memo_hash(dual: Boolean): String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(false, false).toString.getBytes)))
  }
  def options: List[MTQuestionOption] = _options
  def options_=(os: List[MTQuestionOption]) { _options = os }
  def fromXML(x: scala.xml.Node) = {
    Utilities.DebugLog("MTFreeTextQuestion: fromXML:\n" + x.toString,LogLevel.INFO,LogType.ADAPTER,id)

    Symbol((x \\ "Answer" \ "FreeText").text)
  }
  def toXML(is_dual: Boolean, randomize: Boolean) = {
    // is_dual means nothing for this kind of question
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
          <Text>{ text }</Text>
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
  def num_possibilities: BigInt = _num_possibilities

  def num_possibilities_=(n: BigInt) { _num_possibilities = n }
  def allow_empty_pattern_=(ae: Boolean) { _allow_empty = ae }
  def allow_empty_pattern: Boolean = _allow_empty
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
  def regex: Regex = _pattern match { case Some(p) => new Regex(p); case None => "".r }
  def before_filter_=(f: Symbol => Symbol) { _before_filter = f }
  def before_filter: Symbol => Symbol = _before_filter
}