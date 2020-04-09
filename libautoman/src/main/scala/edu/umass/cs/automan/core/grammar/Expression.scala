package edu.umass.cs.automan.core.grammar

import edu.umass.cs.automan.core.grammar.Rank.Grammar
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.info.QuestionType.QuestionType

trait Expression {}

trait TextExpression extends Expression {}

case class Ref(nt: Name) extends TextExpression { // these become NTs by mapping to Expressions
}

case class Binding(nt: Name) extends TextExpression {}

case class Terminal(value: String) extends TextExpression {}

case class Choice(choices: Array[Expression]) extends TextExpression {
  def getOptions: Array[Expression] = choices
}

case class Sequence(sentence: Array[Expression]) extends TextExpression {
  def getSentence: Array[Expression] = sentence
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