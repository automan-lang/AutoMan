package edu.umass.cs.automan.adapters.mturk.mock

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.mock.MockResponse

case class MultiEstimationMockResponse(question_ids: Array[UUID], response_time: Date, answers: Array[Double], worker_id: UUID)
  extends MockResponse(question_ids(0), response_time, worker_id) {

  assert(question_ids.length == answers.length)

  def toXML : String = {
    val xml_decl = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"

    val answer_fields = answers.zipWithIndex.map { case (answer,i) =>
      <Answer>
        <QuestionIdentifier>{ question_ids(i).toString }</QuestionIdentifier>
        <FreeText>{ answer }</FreeText>
      </Answer>
    }

    val assn =
      <QuestionFormAnswers xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd">
        {
          answer_fields
        }
      </QuestionFormAnswers>

    xml_decl + assn.toString()
  }
}
