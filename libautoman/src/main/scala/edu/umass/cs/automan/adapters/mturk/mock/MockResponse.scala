package edu.umass.cs.automan.adapters.mturk.mock

import java.util.UUID

sealed abstract class MockResponse {
  def toXML: String
}

case class RadioButtonMockResponse(question_id: UUID, answer: Symbol) extends MockResponse {
  def toXML : String = {
    val xml_decl = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
    val assn =
      <QuestionFormAnswers xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd">
        <Answer>
          <QuestionIdentifier>{ question_id.toString }</QuestionIdentifier>
          <SelectionIdentifier>{ answer.toString().drop(1) }</SelectionIdentifier>
        </Answer>
      </QuestionFormAnswers>
    xml_decl + assn.toString()
  }
}
case class CheckboxMockResponse(question_id: UUID, answers: Set[Symbol]) extends MockResponse {
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