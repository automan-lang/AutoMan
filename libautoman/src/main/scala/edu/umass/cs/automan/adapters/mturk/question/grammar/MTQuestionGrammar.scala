package edu.umass.cs.automan.adapters.mturk.question.grammar

import edu.umass.cs.automan.core.grammar.{Production, QuestionGrammar}

abstract class MTQuestionGrammar(_rules: Map[String, Production], _startSymbol: String) extends QuestionGrammar(_rules, _startSymbol) {

}


