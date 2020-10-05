package org.automanlang.core.exception

case class OverBudgetException(need: BigDecimal, have: BigDecimal) extends Exception