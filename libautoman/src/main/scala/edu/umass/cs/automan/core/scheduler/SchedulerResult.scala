package edu.umass.cs.automan.core.scheduler

case class SchedulerResult[T](answer: T, cost: BigDecimal, confidence: Double)
