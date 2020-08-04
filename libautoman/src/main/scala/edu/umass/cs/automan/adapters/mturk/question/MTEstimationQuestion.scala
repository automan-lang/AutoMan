package edu.umass.cs.automan.adapters.mturk.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.mturk.mock.EstimationMockResponse
import edu.umass.cs.automan.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import edu.umass.cs.automan.core.logging.DebugLog
import edu.umass.cs.automan.core.logging.LogLevelDebug
import edu.umass.cs.automan.core.logging.LogType
import edu.umass.cs.automan.core.question.{EstimationQuestion, FreeTextQuestion}
import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex

import scala.xml.Node

class MTEstimationQuestion extends EstimationQuestion with MTurkQuestion {
  override type A = EstimationQuestion#A

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    val toRet = new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
    toRet
  }
  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }

  // private API
  _minimum_spawn_policy = MTurkMinimumSpawnPolicy
  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID) : EstimationMockResponse = {
    EstimationMockResponse(question_id, response_time, a, worker_id)
  }
  override protected[mturk] def fromXML(x: scala.xml.Node) : A = {
    // There is a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be34c-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <FreeText>2.11</FreeText>
    //    </Answer>
    DebugLog("MTEstimationQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)

    (x \\ "Answer" \ "FreeText").text.toDouble
  }

  override protected[mturk] def toXML(randomize: Boolean): scala.xml.Node = {
    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
      { XMLBody(randomize) }
    </QuestionForm>
  }

  private def isNumeric : scala.xml.Node = {
    (_min_value, _max_value) match {
      case (Some(min),Some(max)) => <IsNumeric minValue={ Math.floor(min).toInt.toString } maxValue={ Math.ceil(max).toInt.toString } />
      case (Some(min),None) => <IsNumeric minValue={ Math.floor(min).toInt.toString } />
      case (None,Some(max)) => <IsNumeric maxValue={ Math.ceil(max).toInt.toString } />
      case (None,None) => <IsNumeric />
    }
  }

  override protected[automan] def cloneWithConfidence(conf: Double): EstimationQuestion = {
    val mteq = new MTEstimationQuestion
    mteq._before_filter = this._before_filter
    mteq._banned_workers = this._banned_workers
    mteq._budget = this._budget
    mteq._confidence = conf
    mteq._confidence_interval = this._confidence_interval
    mteq._default_sample_size = this._default_sample_size
    mteq._description = this._description
    mteq._dont_randomize_options = this._dont_randomize_options
    mteq._dont_reject = this._dont_reject
    mteq._dry_run = this._dry_run
    mteq._estimator = this._estimator
    mteq._formatted_content = this._formatted_content
    mteq._id = this._id
    mteq._image = this._image
    mteq._image_alt_text = this._image_alt_text
    mteq._image_url = this._image_url
    mteq._initial_worker_timeout_in_s = this._initial_worker_timeout_in_s
    mteq._keywords = this._keywords
    mteq._max_replicas = this._max_replicas
    mteq._max_value = this._max_value
    mteq._min_value = this._min_value
    mteq._minimum_spawn_policy = this._minimum_spawn_policy
    mteq._mock_answers = this._mock_answers
    mteq._payOnFailure = this._payOnFailure
    mteq._price_policy = this._price_policy
    mteq._price_policy_instance = this._price_policy_instance
    mteq._qualifications = this._qualifications
    mteq._qualified_workers = this._qualified_workers
    mteq._question_timeout_multiplier = this._question_timeout_multiplier
    mteq._text = this._text
    mteq._time_value_per_hour = this._time_value_per_hour
    mteq._timeout_policy = this._timeout_policy
    mteq._timeout_policy_instance = this._timeout_policy_instance
    mteq._title = this._title
    mteq._update_frequency_ms = this._update_frequency_ms
    mteq
  }

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def XMLBody(randomize: Boolean): Seq[Node] = {
    Seq(
      toSurveyXML(randomize)
    )
  }

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
            { isNumeric }
          </Constraints>
        </FreeTextAnswer>
      </AnswerSpecification>
    </Question>
  }
}