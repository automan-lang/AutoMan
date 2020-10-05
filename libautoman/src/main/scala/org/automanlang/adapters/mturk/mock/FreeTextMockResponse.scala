package org.automanlang.adapters.mturk.mock

import java.util.{Date, UUID}

import org.automanlang.core.mock.MockResponse

case class FreeTextMockResponse(question_id: UUID, response_time: Date, answer: String, worker_id: UUID)
  extends MockResponse(question_id, response_time, worker_id) {
  def toXML : String = {
    val xml_decl = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
    val assn =
      <QuestionFormAnswers xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd">
        <Answer>
          <QuestionIdentifier>{ question_id.toString }</QuestionIdentifier>
          <FreeText>{ answer }</FreeText>
        </Answer>
      </QuestionFormAnswers>
    xml_decl + assn.toString()
  }
}
