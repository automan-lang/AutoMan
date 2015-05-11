package edu.umass.cs.automan.core.exception

case class OverBudgetException(need: BigDecimal, have: BigDecimal) extends Exception