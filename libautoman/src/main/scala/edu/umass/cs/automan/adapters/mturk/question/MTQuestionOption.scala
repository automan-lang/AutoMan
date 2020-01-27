package edu.umass.cs.automan.adapters.mturk.question

import edu.umass.cs.automan.core.grammar.Grammar
import edu.umass.cs.automan.core.question.QuestionOption

import xml.Unparsed

case class MTQuestionOption(override val question_id: Symbol, override val question_text: String, override val question_grammar: Grammar, image_url: String) extends QuestionOption(question_id: Symbol, question_text: String, question_grammar: Grammar) {
  def toXML: xml.Node = {
    <Selection>
      <SelectionIdentifier>{ question_id.toString().drop(1) }</SelectionIdentifier>
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