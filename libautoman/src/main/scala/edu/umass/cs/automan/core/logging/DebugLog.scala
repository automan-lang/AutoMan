package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}

object DebugLog {
  private var _level : LogLevel = LogLevelInfo()

  def level_=(level: LogLevel) : Unit = { _level = level }
  def level : LogLevel = _level

  def apply(msg: String, level: LogLevel, source: LogType.Value, id: UUID) {
    // only display message if the loglevel is above the user-defined threshold
    if (level.compareTo(_level) >= 0) {
      val idstr =
        id match {
          case null => ""
          case _ =>
            source match {
              case LogType.SCHEDULER => "question_id = " + id.toString + ", "
              case LogType.STRATEGY => "question_id = " + id.toString + ", "
              case LogType.ADAPTER => "question_id = " + id.toString + ", "
              case LogType.MEMOIZER => ""
            }
        }

      System.err.println(new Date().toString + ": " + level.toString + ": " + source.toString + ": " + idstr + msg)
    }
  }
}
