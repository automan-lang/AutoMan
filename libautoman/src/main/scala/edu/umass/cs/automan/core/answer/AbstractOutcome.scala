package edu.umass.cs.automan.core.answer

sealed abstract class AbstractOutcome[T]
case class Outcome[T](value: T) extends AbstractOutcome[T]
case class OverBudget[T]() extends AbstractOutcome[T]