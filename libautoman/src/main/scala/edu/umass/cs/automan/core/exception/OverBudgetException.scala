package edu.umass.cs.automan.core.exception

case class OverBudgetException[B](answer_so_far: Option[B]) extends Exception