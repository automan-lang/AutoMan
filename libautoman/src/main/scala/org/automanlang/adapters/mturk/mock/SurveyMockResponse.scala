package org.automanlang.adapters.mturk.mock

import java.util.{Date, UUID}
import org.automanlang.core.mock.MockResponse

case class SurveyMockResponse(question_id: UUID, response_time: Date, answers: List[Any], worker_id: UUID)
  extends MockResponse(question_id, response_time, worker_id) {
  def toXML : String = {
    val xml_decl = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
    val assn =
      <QuestionFormAnswers xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd">
        { answers.map { answer =>
        <Answer>
          <QuestionIdentifier>{ question_id.toString }</QuestionIdentifier>
          <SelectionIdentifier>{ answer.toString().drop(1) }</SelectionIdentifier>
        </Answer> }
        }
      </QuestionFormAnswers>
    xml_decl + assn.toString()
  }
}
