package org.automanlang.core.logging

sealed trait LogLevel extends Ordered[LogLevel] {
  val _priority = Integer.MAX_VALUE

  override def compare(that: LogLevel): Int = {
    _priority.compareTo(that._priority)
  }
}
case class LogLevelDebug() extends LogLevel {
  override val _priority = 0
  override def toString : String = "DEBUG"
}
case class LogLevelInfo() extends LogLevel {
  override val _priority = 1
  override def toString : String = "INFO"
}
case class LogLevelWarn() extends LogLevel {
  override val _priority = 2
  override def toString : String = "WARN"
}
case class LogLevelFatal() extends LogLevel {
  override val _priority = 3
  override def toString : String = "FATAL"
}
