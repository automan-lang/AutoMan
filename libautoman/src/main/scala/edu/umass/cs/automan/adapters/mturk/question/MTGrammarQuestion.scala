package edu.umass.cs.automan.adapters.mturk.question

import java.util
import java.util.{Date, UUID}

import edu.umass.cs.automan.core.answer.GrammarOutcome
import edu.umass.cs.automan.core.grammar.{Grammar, Ranking, Scope}
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.{CheckboxQuestion, GrammarQuestion, Question}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.xml.Node

class MTGrammarQuestion extends GrammarQuestion with MTurkQuestion {
//  override type A = this.type
//  override type AA = this.type
//  override type AP = this.type

//  type A <: Any			// return type of the function (what you get when you call .value)
//  type AA <: AbstractAnswer[A]	// an instance of scheduler

  //  type AP <: AggregationPolicy	// how to derive a scalar value of type A from the distribution of values
  //  type PP <: PricePolicy	// how to determine reward
  //  type TP <: TimeoutPolicy	// how long to run the job
  //  type PP = MLEPricePolicy
  //  type TP = DoublingTimeoutPolicy

  //override protected var question: Question = _

  override def description: String = _description match { case Some(d) => d; case None => this.title }
  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }

  override def num_possibilities: BigInt = _grammar.count(0, new mutable.HashSet[String]())

  override def generateQuestion(vals: Array[Int], scope: Scope): Question = { ???
//    qType match {
//      case cb: CheckboxQuestion => {
//        var q = new MTCheckboxQuestion
//        q.text = grammar.buildBody(scope, new StringBuilder()).toString()
//        //TODO where are options coming from?
//      }
//    }
  }

  // generate n x m question variants, n = number variants, m = number questions desired
  //override def grammarEval(g: Grammar): Seq[QuestionType] = {
  override def grammarEval(g: Grammar, t: QuestionType): Seq[Question] = {
    var toRet: List[Question] = List[Question]()
    t match {
      case edu.umass.cs.automan.core.info.QuestionType.CheckboxQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.CheckboxDistributionQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.MultiEstimationQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.EstimationQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.FreeTextQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.FreeTextDistributionQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.RadioButtonQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.RadioButtonDistributionQuestion =>
      case edu.umass.cs.automan.core.info.QuestionType.Survey =>
      case edu.umass.cs.automan.core.info.QuestionType.GrammarQuestion =>
    }

    val toGen: Int = numVariants * numQs

    val rand: Random = new Random()
    for(i <- 0 until toGen){
      //val rand: Int =
      val assignment: Array[Int] = Ranking.unrank(rand.nextInt(num_possibilities.toInt), bases).toArray
      val scope: Scope = _grammar.bind(assignment, 0, Set[String]())
      val toAdd: Question = generateQuestion(assignment, scope)
      toRet = toRet :+ toAdd
    }
    toRet
  }

  /**
    * Parses answer from XML
    *
    * @param x the XML
    * @return Answer value
    */
  override protected[mturk] def fromXML(x: Node): MTGrammarQuestion.this.type = ???

  /**
    * Converts question to XML QuestionForm
    * Calls XMLBody
    *
    * @param randomize Randomize option order?
    * @return XML
    */
override protected[mturk] def toXML(randomize: Boolean): Node = ???

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
override protected[mturk] def XMLBody(randomize: Boolean): Seq[Node] = ???
override protected[mturk] def toSurveyXML(randomize: Boolean): Node = ???

override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: MTGrammarQuestion.this.type, worker_id: UUID): MockResponse = ???
override protected[automan] def prettyPrintAnswer(answer: MTGrammarQuestion.this.type): String = ???
}
