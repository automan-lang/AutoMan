package edu.umass.cs.automan.adapters.mturk.question

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.mock.CheckboxMockResponse
import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.CheckboxQuestion
import edu.umass.cs.automan.core.scheduler.BackendResult
import com.amazonaws.mturk.requester.{AssignmentStatus, Assignment}
import edu.umass.cs.automan.core.util.Utilities
import xml.XML
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

class MTCheckboxQuestion extends CheckboxQuestion with MTurkQuestion {
  type QuestionOptionType = MTQuestionOption
  override type A = Set[Symbol]

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def randomized_options: List[QuestionOptionType] = Utilities.randomPermute(options)
  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _group_id match { case Some(g) => g; case None => this.id.toString() }

  // private API
  override protected[mturk] def toMockResponse(question_id: UUID, a: A) : CheckboxMockResponse = {
    CheckboxMockResponse(question_id, a)
  }
  override protected[mturk] def fromXML(x: scala.xml.Node) : A = {
    // There may be MULTIPLE answers here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //      <QuestionIdentifier>721be34c-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>spongebob</SelectionIdentifier>
    //    </Answer>
    DebugLog("MTCheckboxQuestion: fromXML:\n" + x.toString,LogLevel.INFO,LogType.ADAPTER,id)

    (x \\ "Answer" \\ "SelectionIdentifier").map{si => Symbol(si.text)}.toSet
  }
  // TODO: random checkbox fill
  override protected[mturk]def toXML(randomize: Boolean) : scala.xml.Node = {
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
}