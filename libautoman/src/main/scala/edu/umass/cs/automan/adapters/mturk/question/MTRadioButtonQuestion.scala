package edu.umass.cs.automan.adapters.mturk.question

import java.util.{Date, UUID}
import edu.umass.cs.automan.adapters.mturk.mock.RadioButtonMockResponse
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question.RadioButtonQuestion
import edu.umass.cs.automan.core.util.Utilities
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

class MTRadioButtonQuestion extends RadioButtonQuestion with MTurkQuestion {
  type QuestionOptionType = MTQuestionOption
  override type A = Symbol

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def randomized_options: List[QuestionOptionType] = Utilities.randomPermute(options)
  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _group_id match { case Some(g) => g; case None => this.id.toString() }

  // private API
  override def toMockResponse(question_id: UUID, response_time: Date, a: A) : RadioButtonMockResponse = {
    RadioButtonMockResponse(question_id, response_time, a)
  }
  override protected[mturk] def fromXML(x: scala.xml.Node) : A = {
    // There should only be a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //    </Answer>
    DebugLog("MTRadioButtonQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)

    Symbol((x \\ "Answer" \\ "SelectionIdentifier").text)
  }
  // TODO: random checkbox fill
  override protected[mturk]def toXML(randomize: Boolean) : scala.xml.Node = {
    val n = <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
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
    DebugLog("MTRadioButtonQuestion: toXML:\n" + n.toString,LogLevelDebug(),LogType.ADAPTER,id)
    n
  }
}