package org.automanlang.adapters.mturk.question

import io.circe.{HCursor, Json}

import java.util.{Date, UUID}
import org.automanlang.adapters.mturk.mock.FreeTextMockResponse
import org.automanlang.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import org.automanlang.core.logging._
import org.automanlang.core.question.FreeTextQuestion

import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

import scala.xml.Node

class MTFreeTextQuestion extends FreeTextQuestion with MTurkQuestion {
  override type A = FreeTextQuestion#A

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
  override protected[mturk] def fromXML(x: scala.xml.Node) : A = {
    // There is a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be34c-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <FreeText>spongebob</FreeText>
    //    </Answer>
    DebugLog("MTFreeTextQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)

    (x \\ "Answer" \ "FreeText").text
  }
  override protected[mturk] def toXML(randomize: Boolean): scala.xml.Node = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      { toQuestionXML(randomize) }
    </QuestionForm>
  }

  //override type QuestionOptionType = this.type

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

  //override type QuestionOptionType = this.type

  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
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
        <FreeTextAnswer>
          <Constraints>
            <AnswerFormatRegex regex={ regex } errorText={ pattern_error_text } />
          </Constraints>
        </FreeTextAnswer>
      </AnswerSpecification>
    </Question>
  }

  override protected[mturk] def toQuestionHTML(randomize: Boolean): String = {
    // note that here we set margin-bottom to negative to remove the white space at top of <crowd-input>
    s"""
    <div id=${id_string}">
      <div class="QuestionContent" style="margin-bottom: -2em;">
     """ +
      {
        _image_url match {
          case Some(url) => {
            // manual style: max-width / max-height?
            s"""<p><img class=\"question_image\" src=${url} alt=${image_alt_text} /></p>"""
          }
          case None => ""
        }
      } +
      {
        _formatted_content match {
          case Some(x) => {
            x.toString()
          }
          case None => s"<p>${text}</p>"
        }
      } + "</div>" +
      // TODO: enforce min/max with form validation or JS
      s"""<crowd-input name=\"${id_string}\" label=\"${text}\" required></crowd-input>""" +
      "</div>"
  }

  override protected[mturk] def fromHTMLJson(json: Json): A = {
    json.hcursor.as[A].toOption.get
  }
}
