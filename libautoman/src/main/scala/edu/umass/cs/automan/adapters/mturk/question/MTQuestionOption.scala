package edu.umass.cs.automan.adapters.mturk.question

import java.util.UUID
import edu.umass.cs.automan.core.question.QuestionOption
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
      <input type="radio" id={question_id.toString().drop(1)} name={idFromQuestion.toString} value={question_id.toString().drop(1)} required="required"/>
      {toXML(true)}
    </div>
  }
}