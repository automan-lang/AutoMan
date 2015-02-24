package edu.umass.cs.automan.core.logging

import java.util.{Date, UUID}

object DebugLog {
  def apply(msg: String, level: LogLevel.Value, source: LogType.Value, id: UUID) {
    val idstr =
      id match {
        case null => ""
        case _ =>
          source match {
            case LogType.SCHEDULER => "question_id = " + id.toString + ", "
            case LogType.STRATEGY => "computation_id = " + id.toString + ", "
            case LogType.ADAPTER => "question_id = " + id.toString + ", "
            case LogType.MEMOIZER => ""
          }
      }

    System.err.println(new Date().toString + ": " + level.toString + ": " + source.toString + ": " + idstr +  msg)
  }
}
