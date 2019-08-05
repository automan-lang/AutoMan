package edu.umass.cs.automan.core

trait GDSL {
  val automan = edu.umass.cs.automan.automan
  val LogConfig = edu.umass.cs.automan.core.logging.LogConfig
  val Utilities = edu.umass.cs.automan.core.util.Utilities

  // to simplify imports
  type Answer[T] = edu.umass.cs.automan.core.answer.Answer[T]
  type Answers[T] = edu.umass.cs.automan.core.answer.Answers[T]
  type AsymmetricCI = edu.umass.cs.automan.core.question.confidence.AsymmetricCI
  type DistributionOutcome[T] = edu.umass.cs.automan.core.answer.VectorOutcome[T]
  type Estimate = edu.umass.cs.automan.core.answer.Estimate
  type IncompleteAnswers[T] = edu.umass.cs.automan.core.answer.IncompleteAnswers[T]
  type LowConfidenceAnswer[T] = edu.umass.cs.automan.core.answer.LowConfidenceAnswer[T]
  type LowConfidenceEstimate = edu.umass.cs.automan.core.answer.LowConfidenceEstimate
  type OverBudgetAnswer[T] = edu.umass.cs.automan.core.answer.OverBudgetAnswer[T]
  type OverBudgetAnswers[T] = edu.umass.cs.automan.core.answer.OverBudgetAnswers[T]
  type OverBudgetEstimate = edu.umass.cs.automan.core.answer.OverBudgetEstimate
  type ScalarOutcome[T] = edu.umass.cs.automan.core.answer.ScalarOutcome[T]
  type Outcome[T] = edu.umass.cs.automan.core.answer.Outcome[T]

  // to simplify pattern matching
  val Answer = edu.umass.cs.automan.core.answer.Answer
  val Answers = edu.umass.cs.automan.core.answer.Answers
  val Estimate = edu.umass.cs.automan.core.answer.Estimate
  val IncompleteAnswers = edu.umass.cs.automan.core.answer.IncompleteAnswers
  val LowConfidenceAnswer = edu.umass.cs.automan.core.answer.LowConfidenceAnswer
  val LowConfidenceEstimate = edu.umass.cs.automan.core.answer.LowConfidenceEstimate
  val OverBudgetAnswer = edu.umass.cs.automan.core.answer.OverBudgetAnswer
  val OverBudgetAnswers = edu.umass.cs.automan.core.answer.OverBudgetAnswers
  val OverBudgetEstimate = edu.umass.cs.automan.core.answer.OverBudgetEstimate
  val SymmetricCI = edu.umass.cs.automan.core.question.confidence.SymmetricCI
  val UnconstrainedCI = edu.umass.cs.automan.core.question.confidence.UnconstrainedCI
}
