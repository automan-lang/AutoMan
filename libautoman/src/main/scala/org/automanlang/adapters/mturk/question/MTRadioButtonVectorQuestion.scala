package org.automanlang.adapters.mturk.question

import java.util.{Date, UUID}

import org.automanlang.adapters.mturk.mock.RadioButtonMockResponse
import org.automanlang.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import org.automanlang.core.logging._
import org.automanlang.core.question.RadioButtonVectorQuestion
import org.automanlang.core.util.Utilities
import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex

import scala.xml.Node

class MTRadioButtonVectorQuestion extends RadioButtonVectorQuestion with MTurkQuestion {
  type QuestionOptionType = MTQuestionOption
  override type A = RadioButtonVectorQuestion#A

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes))) //TODO default?
  }

  override def description: String = _description match {
    case Some(d) => d;
    case None => this.title
  }

  override def group_id: String = _title match {
    case Some(t) => t;
    case None => this.id.toString
  }

  override def randomized_options: List[QuestionOptionType] = Utilities.randomPermute(options)

  // private API
  _minimum_spawn_policy = MTurkMinimumSpawnPolicy

  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID): RadioButtonMockResponse = {
    RadioButtonMockResponse(question_id, response_time, a, worker_id)
  }

  override protected[mturk] def fromXML(x: scala.xml.Node): A = {
    // There should only be a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //    </Answer>
    DebugLog("MTRadioButtonDistributionQuestion: fromXML:\n" + x.toString, LogLevelDebug(), LogType.ADAPTER, id)

    Symbol((x \\ "Answer" \\ "SelectionIdentifier").text)
  }

  override protected[mturk] def toXML(randomize: Boolean): scala.xml.Node = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      {toQuestionXML(randomize)}
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
      <Question>
        <QuestionIdentifier>
          {if (randomize) id_string else ""}
        </QuestionIdentifier>
        <IsRequired>true</IsRequired>
        <QuestionContent>
          {_image_url match {
          case Some(url) => {
            <Binary>
              <MimeType>
                <Type>image</Type>
                <SubType>png</SubType>
              </MimeType>
              <DataURL>
                {url}
              </DataURL>
              <AltText>
                {image_alt_text}
              </AltText>
            </Binary>
          }
          case None => {}
        }}{// if formatted content is specified, use that instead of text field
          _formatted_content match {
            case Some(x) => <FormattedContent>
              {scala.xml.PCData(x.toString)}
            </FormattedContent>
            case None => <Text>
              {text}
            </Text>
          }}
        </QuestionContent>
        <AnswerSpecification>
          <SelectionAnswer>
            <StyleSuggestion>radiobutton</StyleSuggestion>
            <Selections>
              {if (randomize) randomized_options.map {
              _.toXML(false)
            } else options.map {
              _.toXML(false)
            }}
            </Selections>
          </SelectionAnswer>
        </AnswerSpecification>
      </Question>
    )
  }

  override protected[mturk] def toSurveyXML(randomize: Boolean) = ???
}