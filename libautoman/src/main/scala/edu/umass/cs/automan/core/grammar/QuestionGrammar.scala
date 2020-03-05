package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.question.Question

abstract class QuestionGrammar(_rules: Map[String, Production], _startSymbol: String, _maxDepth: Int) extends Grammar(_rules, _startSymbol, _maxDepth){
  def toQuestion(i: Int): Question
}

abstract class RadioButtonQuestionGrammar(override val _rules: Map[String, Production], override val _startSymbol: String, override val _maxDepth: Int) extends QuestionGrammar(_rules, _startSymbol, _maxDepth)
  //override def toQuestion(i: Int): Question = ???
