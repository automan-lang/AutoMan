package org.automanlang.core.logging

object LogType extends Enumeration {
  type LogType = Value
  val STRATEGY,
  SCHEDULER,
  ADAPTER,
  MEMOIZER
  = Value
}
