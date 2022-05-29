package org.automanlang.adapters.mturk.question

import java.util.UUID
import org.automanlang.core.question.QuestionOption

import java.security.InvalidParameterException
import xml.Unparsed

case class MTQuestionOption(override val question_id: Symbol, override val question_text: String, image_url: String) extends QuestionOption(question_id: Symbol, question_text: String) {
  def toXML(isSurvey: Boolean): xml.Node = {
    if (isSurvey) {
      <label for={question_id.toString().drop(1)}>
        { if(image_url != "") {
        <p><img id="question_image" src={ image_url }/></p>
      }
      { question_text }
        }
      </label>
    } else {
      <Selection>
        <SelectionIdentifier>{ question_id.toString() }</SelectionIdentifier>
        { if(image_url != "") {
        <FormattedContent>
          { Unparsed("<![CDATA[<table><tr><td><img src=\"" + image_url + "\" alt=\"" + question_text + "\"></img></td><td>" + question_text + "</td></tr></table>]]>") }
        </FormattedContent>
      } else {
        <Text>{ question_text }</Text>
      } }
      </Selection>
    }
  }

  /**
   * This helper method is only called from Surveys.
   * @param idFromQuestion
   * @return
   */
  def toSurveyXML(idFromQuestion: UUID): xml.Node = {
    <div name="opt">
      <input type="radio" id={question_id.toString()} name={idFromQuestion.toString} value={question_id.toString()} />
      {toXML(true)}
    </div>
  }

  def toSurveyHTML(idFromQuestion: String, inputType: String): String = {
    // prefix added to question_id to make it unique
    var prefix: String = idFromQuestion + "-"

    if (inputType == "radio") {
      s"""<div>
         |<input type="radio" id=\"${prefix + question_id.toString()}\" name=\"${idFromQuestion}\" value=\"${question_id.toString()}\" required />
         |<label for=\"${prefix + question_id.toString()}\">""".stripMargin +
        {
          if(image_url != "") {
            "<table><tr><td><img src=\"" + image_url + "\" alt=\"" + question_text + "\"></img></td><td>" + question_text + "</td></tr></table>"
          } else {
            question_text
          }
        } +
        "</label></div>"
    } else if (inputType == "checkbox") {
      // Cannot required here or it'll make everything required
      s"""<div><crowd-checkbox name=\"${idFromQuestion}\" value=\"${question_id.toString()}\">""" +
        {
          if(image_url != "") {
            "<table><tr><td><img src=\"" + image_url + "\" alt=\"" + question_text + "\"></img></td><td>" + question_text + "</td></tr></table>"
          } else {
            question_text
          }
        } + "</crowd-checkbox></div>"
    } else {
      throw new InvalidParameterException
    }
  }
}
