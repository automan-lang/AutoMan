package org.automanlang.adapters.mturk.question

import java.util.{Date, UUID}

import org.automanlang.adapters.mturk.mock.FreeTextMockResponse
import org.automanlang.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import org.automanlang.core.logging._
import java.security.MessageDigest

import org.automanlang.core.question.FileVectorQuestion
import org.apache.commons.codec.binary.Hex

import scala.xml.Node

class MTFileVectorQuestion extends FileVectorQuestion with MTurkQuestion {
  type QuestionOptionType = MTQuestionOption
  override type A = FileVectorQuestion#A

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def description: String = _description match { case Some(d) => d; case None => this.title }
  //  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }
  override def group_id: String = title

  // private API
  _minimum_spawn_policy = MTurkMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID) : FreeTextMockResponse = {
    FreeTextMockResponse(question_id, response_time, a, worker_id)
  }
  def fromXML(x: scala.xml.Node) : A = {
    // There is a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be34c-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <FreeText>spongebob</FreeText>
    //    </Answer>
    DebugLog("MTFreeTextDistributionQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)

    (x \\ "Answer" \ "FreeText").text
  }
  override protected[mturk] def toXML(randomize: Boolean): scala.xml.Node = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      { toQuestionXML(randomize) }
    </QuestionForm>
  }

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def toQuestionXML(randomize: Boolean): Seq[Node] = {
    Seq(
      toSurveyXML(randomize)
    )
  }

  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
//    <Question>
//      <QuestionIdentifier>{ if (randomize) id_string else "" }</QuestionIdentifier>
//      <QuestionContent>
//        {
//        _image_url match {
//          case Some(url) => {
//            <Binary>
//              <MimeType>
//                <Type>image</Type>
//                <SubType>png</SubType>
//              </MimeType>
//              <DataURL>{ url }</DataURL>
//              <AltText>{ image_alt_text }</AltText>
//            </Binary>
//          }
//          case None => {}
//        }
//        }
//        {
//        // if formatted content is specified, use that instead of text field
//        _formatted_content match {
//          case Some(x) => <FormattedContent>{ scala.xml.PCData(x.toString()) }</FormattedContent>
//          case None => <Text>{ text }</Text>
//        }
//        }
//      </QuestionContent>
//      <AnswerSpecification>
//        <FreeTextAnswer>
//          <Constraints>
//            <AnswerFormatRegex regex={ this.regex } errorText={ pattern_error_text } />
//          </Constraints>
//        </FreeTextAnswer>
//      </AnswerSpecification>
//    </Question>
    <Question>
      <FileUploadAnswer>
        <MaxFileSizeInBytes>3000000</MaxFileSizeInBytes>
        <MinFileSizeInBytes>1000</MinFileSizeInBytes>
      </FileUploadAnswer>
    </Question>
  }
}