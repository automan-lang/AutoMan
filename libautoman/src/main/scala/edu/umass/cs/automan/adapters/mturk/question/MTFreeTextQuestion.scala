package edu.umass.cs.automan.adapters.mturk.question

import java.util.UUID
import edu.umass.cs.automan.adapters.mturk.mock.FreeTextMockResponse
import edu.umass.cs.automan.core.logging.{LogType, LogLevel, DebugLog}
import edu.umass.cs.automan.core.question.FreeTextQuestion
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

class MTFreeTextQuestion extends FreeTextQuestion with MTurkQuestion {
  override type A = String

  protected var _before_filter: Symbol => Symbol = (s) => s

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _group_id match { case Some(g) => g; case None => this.id.toString() }

  // private API
  override protected[mturk] def toMockResponse(question_id: UUID, a: A) : FreeTextMockResponse = {
    FreeTextMockResponse(question_id, a)
  }
  def fromXML(x: scala.xml.Node) : A = {
    // There is a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be34c-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <FreeText>spongebob</FreeText>
    //    </Answer>
    DebugLog("MTFreeTextQuestion: fromXML:\n" + x.toString,LogLevel.INFO,LogType.ADAPTER,id)

    (x \\ "Answer" \ "FreeText").text
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