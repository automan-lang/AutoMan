package edu.umass.cs.automan.adapters.MTurk.question

import edu.umass.cs.automan.core.question._
import actors.Future
import edu.umass.cs.automan.core.answer.CheckboxAnswer
import java.util.UUID
import xml.XML
import com.amazonaws.mturk.requester.Assignment
import edu.umass.cs.automan.core.scheduler.Thunk
import edu.umass.cs.automan.adapters.MTurk.{AutomanHIT, MTurkAdapter}
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import edu.umass.cs.automan.core.{LogType, LogLevel, Utilities}

object MTCheckboxQuestion {
  def apply(init: MTCheckboxQuestion => Unit, a: MTurkAdapter) : Future[CheckboxAnswer] = {
    val checkbox_question = new MTCheckboxQuestion
    init(checkbox_question)
    a.schedule(checkbox_question)
  }
}

class MTCheckboxQuestion extends CheckboxQuestion[MTQuestionOption] with MTurkQuestion {
  protected var _options = List[MTQuestionOption]()

  def answer(a: Assignment, is_dual: Boolean): CheckboxAnswer = {
    val ans_symb = fromXML(XML.loadString(a.getAnswer))
    if (is_dual) {
      // get all possible symbols
      val all_symb = options.map{ o => o.question_id }.toSet
      // answer is set difference
      new CheckboxAnswer(None, a.getWorkerId, all_symb &~ ans_symb)
    } else {
      new CheckboxAnswer(None, a.getWorkerId, ans_symb)
    }
  }
  def build_hit(ts: List[Thunk], dual: Boolean) : AutomanHIT = {
    val x = toXML(dual, true)
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
    new String(Hex.encodeHex(md.digest(toXML(dual, false).toString.getBytes)))
  }
  def options: List[MTQuestionOption] = _options
  def options_=(os: List[MTQuestionOption]) { _options = os }
  def fromXML(x: scala.xml.Node) : Set[Symbol] = {
    (x \\ "Answer" \\ "SelectionIdentifier").map{si => Symbol(si.text)}.toSet
  }
  def randomized_options: List[MTQuestionOption] = {
    import edu.umass.cs.automan.core.Utilities
    Utilities.randomPermute(options)
  }
  def toXML(is_dual: Boolean, randomize: Boolean) = {
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
          <Text>{ if(is_dual) dual_text else text }</Text>
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
}
