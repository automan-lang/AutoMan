package edu.umass.cs.automan.core.exception

case class OverBudgetException[T](answer: T, cost: BigDecimal) extends Exception