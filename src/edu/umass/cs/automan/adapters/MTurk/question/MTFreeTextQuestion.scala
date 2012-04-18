package edu.umass.cs.automan.adapters.MTurk.question

import edu.umass.cs.automan.adapters.MTurk.{AutomanHIT, MTurkAdapter}
import actors.Future
import edu.umass.cs.automan.core.scheduler.Thunk
import com.amazonaws.mturk.requester.Assignment
import xml.XML
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import edu.umass.cs.automan.core.question.{FreeTextQuestion, RadioButtonQuestion}
import util.matching.Regex
import edu.umass.cs.automan.core.answer.{FreeTextAnswer, RadioButtonAnswer}
import edu.umass.cs.automan.core.strategy.PictureClause

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
  protected var _num_possibilities: BigInt = 1000

  def answer(a: Assignment, is_dual: Boolean): RadioButtonAnswer = {
    // ignore is_dual
    new RadioButtonAnswer(None, a.getWorkerId, fromXML(XML.loadString(a.getAnswer)))
  }
  def build_hit(ts: List[Thunk], is_dual: Boolean) : AutomanHIT = {
    // we ignore the "dual" option here
    val x = toXML(false, true)
    val h = AutomanHIT { a =>
      a.hit_type_id = _hit_type_id
      a.title = text
      a.description = text
      a.keywords = _keywords
      a.question_xml = x
      a.assignmentDurationInSeconds = _worker_timeout_in_s
      a.lifetimeInSeconds = question_timeout_in_s
      a.maxAssignments = ts.size
      a.cost = ts.head.cost
    }
    println(x)
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
    println("DEBUG: MTFreeTextQuestion: fromXML: ")
    println(x.toString())

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
  def pattern: String = _pattern match { case Some(p) => p; case None => ".*" }
  def pattern_=(p: String) {
    PictureClause(p) match {
      case (regex, count) => {
        _pattern = Some(regex)
        _num_possibilities = if (count > 1000) 1000 else count
      }
    }
  }
  def regex: Regex = _pattern match { case Some(p) => new Regex(p); case None => "".r }
}