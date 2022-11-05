package org.automanlang.core.question

import org.automanlang.core.grammar.{CheckboxQuestionProduction, EstimateQuestionProduction, FreetextQuestionProduction, QuestionProduction, RadioQuestionProduction}
import org.automanlang.core.grammar.Rank.Grammar
import org.automanlang.core.info.QuestionType.{CheckboxDistributionQuestion, CheckboxQuestion, EstimationQuestion, FreeTextDistributionQuestion, FreeTextQuestion, MultiEstimationQuestion, QuestionType, RadioButtonDistributionQuestion, RadioButtonQuestion, Survey, VariantQuestion}

abstract class GrammarSurvey extends FakeSurvey {
  protected var _grammar: List[Grammar]
  protected var _types: List[QuestionType]
  protected var _variant: List[Int]
  protected var _depth: Int

  def grammars: List[Grammar] = _grammar
  def grammars_=(g: List[Grammar]): Unit = {
    _grammar = g
  }

  def variant: List[Int] = _variant
  def variant_=(v: List[Int]): Unit = {
    _variant = v
  }

  def depth: Int = _depth
  def depth_=(d: Int): Unit = {
    _depth = d
  }

  def types: List[QuestionType] = _types
  def types_=(t: List[QuestionType]): Unit = {
    _types = t
  }

  def questionProduction(): List[QuestionProduction] = {
    types.zipWithIndex.map{ case (questionType, i) => questionType match {
      case CheckboxQuestion => CheckboxQuestionProduction(grammars(i))
      case EstimationQuestion => EstimateQuestionProduction(grammars(i))
      case FreeTextQuestion => FreetextQuestionProduction(grammars(i))
      case RadioButtonQuestion => RadioQuestionProduction(grammars(i))
    } }
  }
}
