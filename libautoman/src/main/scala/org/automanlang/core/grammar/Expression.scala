package org.automanlang.core.grammar

import Rank.Grammar
import org.automanlang.core.info.QuestionType
import org.automanlang.core.info.QuestionType.QuestionType


trait Expression {}

trait TextExpression extends Expression {}

/**
  * A Ref is an Expression that refers to another Expression in a grammar.
  * @param nt The Name of the Expression
  */
case class Ref(nt: Name) extends TextExpression { // these become NTs by mapping to Expressions
  def getName = nt
}

/**
  * A Binding refers to another Expression, but always evaluates to the same thing.
  * @param nt The Name of the Binding
  */
case class Binding(nt: Name) extends TextExpression {}

/**
  * A Terminal contains a String.
  * @param value The String the Terminal is associated with
  */
case class Terminal(value: String) extends TextExpression {
  def toText = value
}

/**
  * A Choice contains multiple options that it could evaluate to. Which one is determined by the instance assignment.
  * @param choices The options of the Choice
  */
case class Choice(choices: Array[Expression]) extends TextExpression {
  def getOptions: Array[Expression] = choices
}

/**
  * A Sequence is a combination of Productions.
  * @param sentence A list of Productions
  */
case class Sequence(sentence: Array[Expression]) extends TextExpression {
  def getSentence: Array[Expression] = sentence
}

/**
  * Functions are Productions with a value that varies based on the value of an associated Name.
  * A sample Function could map names to pronouns, or nouns to articles.
  * @param fun The mapping of Names to values
  * @param param The Ref the Function is associated with. Note that it must refer to a Binding.
  * @param capitalize Whether or not to capitalize the value
  */
case class Function(fun: Map[String, String], param: Name, capitalize: Boolean) extends TextExpression {

  // "Call" the function on the string
  def runFun(s: String): String = {
    if(capitalize) fun(s).capitalize
    else fun(s)
  }
}

/*** Question Productions ***/
abstract class QuestionProduction(g: Grammar) extends Expression {
  val _questionType: QuestionType

  def questionType: QuestionType = _questionType

  // returns tuple (body text, options list)
  def toQuestionText(variation: Int, depth: Int): (String, List[String])
}

case class OptionProduction(text: TextExpression) extends Expression {}

case class EstimateQuestionProduction(g: Grammar) extends QuestionProduction(g) {
  override val _questionType: QuestionType = QuestionType.EstimationQuestion

  override def toQuestionText(variation: Int, depth: Int): (String, List[String]) = ???
}

case class CheckboxQuestionProduction(g: Grammar) extends QuestionProduction(g) {
  override val _questionType: QuestionType = QuestionType.CheckboxQuestion

  override def toQuestionText(variation: Int, depth: Int): (String, List[String]) = ???
}

case class CheckboxesQuestionProduction(g: Grammar) extends QuestionProduction(g) {
  override val _questionType: QuestionType = QuestionType.CheckboxDistributionQuestion

  override def toQuestionText(variation: Int, depth: Int): (String, List[String]) = ???
}

case class FreetextQuestionProduction(g: Grammar) extends QuestionProduction(g) {
  override val _questionType: QuestionType = QuestionType.FreeTextQuestion

  override def toQuestionText(variation: Int, depth: Int): (String, List[String]) = ???
}

case class FreetextsQuestionProduction(g: Grammar) extends QuestionProduction(g) {
  override val _questionType: QuestionType = QuestionType.FreeTextQuestion

  override def toQuestionText(variation: Int, depth: Int): (String, List[String]) = ???
}

case class RadioQuestionProduction(g: Grammar) extends QuestionProduction(g) {
  override val _questionType: QuestionType = QuestionType.RadioButtonQuestion

  override def toQuestionText(variation: Int, depth: Int): (String, List[String]) = {
    val (body, opts) = Render.buildInstance(g, variation, depth)
    (body, opts.toList)
  }
}

case class RadiosQuestionProduction(g: Grammar) extends QuestionProduction(g) {
  override val _questionType: QuestionType = QuestionType.RadioButtonDistributionQuestion

  override def toQuestionText(variation: Int, depth: Int): (String, List[String]) = ???
}
