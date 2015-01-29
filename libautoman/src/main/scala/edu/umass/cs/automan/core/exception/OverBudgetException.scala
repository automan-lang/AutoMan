package edu.umass.cs.automan.core.exception

case class OverBudgetException[T](answer_so_far: Option[T]) extends Exception