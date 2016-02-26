package edu.umass.cs.automan.core.mock

import java.util.UUID

case class MockAnswer[T](answer: T, time_delta_in_ms: Long, worker_id: UUID)
