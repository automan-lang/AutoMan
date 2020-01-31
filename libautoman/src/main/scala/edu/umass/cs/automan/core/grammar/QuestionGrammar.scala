package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.question.Question

abstract class QuestionGrammar(_rules: Map[String, Production], _startSymbol: String) extends Grammar(_rules, _startSymbol){
  def toQuestion(i: Int): Question
}

abstract class RadioButtonQuestionGrammar(override val _rules: Map[String, Production], override val _startSymbol: String) extends QuestionGrammar(_rules, _startSymbol)
  //override def toQuestion(i: Int): Question = ???
