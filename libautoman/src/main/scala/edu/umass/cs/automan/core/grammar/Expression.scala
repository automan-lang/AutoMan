package edu.umass.cs.automan.core.grammar

trait Expression {}

case class NonTerminal(name: String, expression: Expression) extends Expression {

}

case class Binding(name: String, expression: Expression) extends Expression {

}

case class Terminal(value: String) extends Expression {

}

case class Choice(choices: Array[Expression]) extends Expression {

}

case class Sequence(sentence: Array[Expression]) extends Expression {

}
