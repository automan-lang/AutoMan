package edu.umass.cs.automan.core.question
import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.mturk.question.MTVariantQuestion
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractSurveyAnswer, Answers, Outcome, SurveyAnswers, SurveyOutcome}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.policy.aggregation.{SimpleSurveyPolicy, SurveyPolicy, VectorPolicy}
import edu.umass.cs.automan.core.policy.price.MLEPricePolicy
import edu.umass.cs.automan.core.policy.timeout.DoublingTimeoutPolicy

// abstract Survey class (will implement in adapters)
abstract class Survey extends Question {
  type A = Set[(String,Question#A)] //Set[Map[String, String]] or String? // the return type (.value) TODO: maybe refine String
  type AA = SurveyAnswers
  type O = SurveyOutcome // outcome type
  type AP = SimpleSurveyPolicy
  type PP = MLEPricePolicy
  type TP = DoublingTimeoutPolicy

  protected var _question_list: List[Outcome[_]] = List() // the list of questions in the survey // TODO DSL
  private var _sample_size: Int = 30

  def question_list_=(l: List[Outcome[_]]) { _question_list = l }
  def question_list: List[Outcome[_]] = _question_list
  def sample_size_=(n: Int) { _sample_size = n }
  def sample_size : Int = _sample_size

  // hash of something in Q so memoizer knows when Qs are the same; def at implementation level
  //override def memo_hash: String = ???

  // TODO: AP SurveyPolicy? (Will just accept answers for now; later may reject/mark outliers)
  override private[automan] def init_validation_policy(): Unit = {
    _validation_policy_instance = _validation_policy match {
      case None => new AP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override private[automan] def init_price_policy(): Unit = {
    _price_policy_instance = _price_policy match {
      case None => new PP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  override private[automan] def init_timeout_policy(): Unit = {
    _timeout_policy_instance = _timeout_policy match {
      case None => new TP(this)
      case Some(policy) => policy.getConstructor(classOf[Question]).newInstance(this)
    }
  }

  protected[automan] def getOutcome(adapter: AutomanAdapter): O = {
    SurveyOutcome(this, schedulerFuture(adapter)).asInstanceOf[O]
  }

  override protected[automan] def prettyPrintAnswer(answer: A) : String = {
    val ansString: StringBuilder = new StringBuilder()
    val ansMap: Map[String, Question#A] = answer.toMap
    for(o <- _question_list) {
      o match {
        case vq: VariantQuestion => {
          val ans = vq.prettyPrintAnswer(ansMap(vq.newQ.id.toString).asInstanceOf[vq.A])
          ansString.append(ans + "\n")
          //          val ans: Question#A = ansMap(vq.newQ.id.toString) // this is just a question identifier
          //          val ppans = q.prettyPrintAnswer(ans.asInstanceOf[q.A])
          //          ansString.append(ppans) //A: Set[(String,Question#A)] // so this is also getting the question ID
        }
        case q: Question => {
          val ans: Question#A = ansMap(q.id.toString)
          val ppans = q.prettyPrintAnswer(ans.asInstanceOf[q.A])
          ansString.append(ppans + "\n") //A: Set[(String,Question#A)]
        }
      }
//      val q: Question = o.question
//      val ans: Question#A = ansMap(q.id.toString)
//      val ppans = q.prettyPrintAnswer(ans.asInstanceOf[q.A])
//      println(s"printing ${q.id} answer: ${ppans}")
//      ansString.append(ppans) //A: Set[(String,Question#A)]
    }
    ansString.toString()
  }

  // TODO: Do we need?
  protected[automan] def composeOutcome(o: O, adapter: AutomanAdapter): O = ???
  //{
//    // unwrap future from previous Outcome
//    val f = o.f map {
//      case Answers(values, _, id, dist) =>
//        Answers(
//          values,
//          BigDecimal(0.00).setScale(2, math.BigDecimal.RoundingMode.FLOOR),
//          id,
//          dist
//        )
//      case _ => startScheduler(adapter)
//
//    }
//    SurveyOutcome(this, f).asInstanceOf[O]
//  }

  //TODO: What will this look like for survey?
  protected[automan] def getQuestionType: QuestionType = QuestionType.Survey
}
