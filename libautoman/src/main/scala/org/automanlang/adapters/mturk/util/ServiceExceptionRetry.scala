package org.automanlang.adapters.mturk.util

import com.amazonaws.services.mturk.model.ServiceException
import org.automanlang.core.logging.{DebugLog, LogLevelWarn, LogType}

object ServiceExceptionRetry {
  @annotation.tailrec
  def apply[T](n: Int)(fn: => T): T = {
    try {
      fn
    } catch {
      case e: ServiceException => {
        DebugLog(s"MTurk service temporarily unavailable: ${e.getMessage}",LogLevelWarn(),LogType.ADAPTER,null)
        ServiceExceptionRetry(n - 1)(fn)
      }
    }
  }
}