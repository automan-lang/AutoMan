package edu.umass.cs.automan.core.exception

import edu.umass.cs.automan.core.scheduler.SchedulerResult

case class OverBudgetException[T](result: Option[SchedulerResult[T]]) extends Exception