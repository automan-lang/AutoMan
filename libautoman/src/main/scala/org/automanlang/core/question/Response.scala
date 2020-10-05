package org.automanlang.core.question

case class Response[T](value: T, worker_id: String)
