package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.Rank.{Grammar, Name}

trait Expression {}

trait TextExpression extends Expression {}

case class Ref(nt: Name) extends TextExpression { // these become NTs by mapping to Expressions
}

case class Binding(nt: Name) extends TextExpression {}

case class Terminal(value: String) extends TextExpression {}

case class Choice(choices: Array[Expression]) extends TextExpression {}

case class Sequence(sentence: Array[Expression]) extends TextExpression {}

/*** Question Productions ***/
abstract class QuestionProduction(g: Grammar) extends Expression {}

class OptionProduction(text: TextExpression) extends Expression {}

class EstimateQuestionProduction(g: Grammar) extends QuestionProduction(g) {}

class EstimatesQuestionProduction(g: Grammar) extends QuestionProduction(g) {}

class CheckboxQuestionProduction(g: Grammar) extends QuestionProduction(g) {}

class CheckboxesQuestionProduction(g: Grammar) extends QuestionProduction(g) {}

class FreetextQuestionproduction(g: Grammar) extends QuestionProduction(g) {}

class FreetextsQuestionproduction(g: Grammar) extends QuestionProduction(g) {}

class RadioQuestionProduction(g: Grammar) extends QuestionProduction(g) {}

class RadiosQuestionProduction(g: Grammar) extends QuestionProduction(g) {}