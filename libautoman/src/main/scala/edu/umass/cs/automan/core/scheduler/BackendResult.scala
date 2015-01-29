package edu.umass.cs.automan.core.scheduler

case class BackendResult[T](answer: T, cost: BigDecimal, worker_id: String)