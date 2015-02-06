package edu.umass.cs.automan.core.scheduler

import java.util.Date

case class BackendResult[T](answer: T, worker_id: String, accept_time: Date, submit_time: Date)