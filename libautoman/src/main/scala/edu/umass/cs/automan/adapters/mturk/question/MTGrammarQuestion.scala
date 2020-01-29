package edu.umass.cs.automan.adapters.mturk.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.core.info.QuestionType.QuestionType
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.question.{CheckboxQuestion, GrammarQuestion, Question}

import scala.xml.Node

class MTGrammarQuestion extends GrammarQuestion with MTurkQuestion {
  override protected var question: Question = _

  override def num_possibilities: BigInt = ???

  override def generateQuestion(vals: Array[Int], qType: QuestionType): Question = {
    qType match {
      case cb: CheckboxQuestion => {
        var q = new MTCheckboxQuestion
        q.text = grammar.buildBody(scope, new StringBuilder()).toString()
        //TODO where are options coming from?
      }
    }
  }

  override type A = this.type

  override def description: String = ???

  override def group_id: String = ???

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

  override type AA = this.type
  override type AP = this.type
  override type PP = this.type
  override type TP = this.type
override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: MTGrammarQuestion.this.type, worker_id: UUID): MockResponse = ???
override protected[automan] def prettyPrintAnswer(answer: MTGrammarQuestion.this.type): String = ???
}
