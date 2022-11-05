package org.automanlang.adapters.mturk.question

import io.circe.{HCursor, Json}

import java.util.{Date, UUID}
import org.automanlang.adapters.mturk.mock.CheckboxMockResponse
import org.automanlang.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import org.automanlang.core.logging._
import org.automanlang.core.question.CheckboxQuestion
import org.automanlang.core.util.Utilities

import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

import java.util
import scala.xml.{Node, NodeSeq}

class MTCheckboxQuestion extends CheckboxQuestion with MTurkQuestion {
  type QuestionOptionType = MTQuestionOption
  override type A = CheckboxQuestion#A

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def randomized_options: List[QuestionOptionType] = Utilities.randomPermute(options)
  override def description: String = _description match { case Some(d) => d; case None => this.title }
//  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }
  override def group_id: String = title
  
  // private API
  _minimum_spawn_policy = MTurkMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID) : CheckboxMockResponse = {
    CheckboxMockResponse(question_id, response_time, a, worker_id)
  }
  override protected[mturk] def fromXML(x: scala.xml.Node) : A = { // CheckboxQuestion#A
    // There may be MULTIPLE answers here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //      <QuestionIdentifier>721be34c-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>spongebob</SelectionIdentifier>
    //    </Answer>
    DebugLog("MTCheckboxQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)

    (x \\ "Answer" \\ "SelectionIdentifier").map{si => Symbol(si.text.drop(1))}.toSet
  }
  // TODO: random checkbox fill
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
          <Selections>{ if(randomize) randomized_options.map { _.toXML(false) } else options.map { _.toXML(false) } }</Selections>
        </SelectionAnswer>
      </AnswerSpecification>
    </Question>
  }

  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
    <div id={id.toString} class="question">
      {
        _image_url match {
          case Some(url) => <p><img id="question_image" src={ url }/></p>
          case None => NodeSeq.Empty
        }
      }
      {
        _text match {
          case Some(text) => <p>{ text }</p>
          case None => NodeSeq.Empty
        }
      }
      <div id={s"opts_${id.toString}"}>
        {options.map(_.toSurveyXML(id))}
      </div>
    </div>
  }

  override protected[mturk] def toQuestionHTML(randomize: Boolean): String = {
    s"""
    <div id="${id_string}">
      <div class="QuestionContent">
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
      } + s"""
      </div>
      ${options.map(_.toSurveyHTML(id_string, "checkbox")).mkString}
    </div>
    """
  }

  override protected[mturk] def fromHTMLJson(json: Json) : A = {
    // {"'cookiemonster":true,"'kermit":false,"'oscar":false,"'spongebob":false,"'thecount":false}

    var ans: A = Set[Symbol]()

    val cursor: HCursor = json.hcursor
    cursor.keys.get.foreach(k => {
      val selected: Boolean = json.hcursor.downField(k).as[Boolean].toOption.get

      if (selected)
        ans += Symbol(k.drop(1))
    })

    ans
  }
}
