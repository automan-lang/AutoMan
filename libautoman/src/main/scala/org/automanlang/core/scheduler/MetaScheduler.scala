package org.automanlang.core.scheduler

import org.automanlang.core.AutomanAdapter
import org.automanlang.core.logging.{LogType, LogLevelInfo, DebugLog}
import org.automanlang.core.question.{Question, MetaQuestion}

class MetaScheduler(val metaQ: MetaQuestion, backend: AutomanAdapter) {
  def run() : MetaQuestion#MAA = {
    var round = 1
    var done = false
    var outcome: Option[MetaQuestion#MAA] = None

    while(!done) {
      if (round != 1) {
        DebugLog(s"Starting round = $round for combined estimate.",
          LogLevelInfo(),
          LogType.SCHEDULER,
          null
        )
      }

      // compute answer
      val answer = metaQ.metaAnswer(1, backend)

      // joint constraints met?
      done = metaQ.done(round, backend)

      if (done) {
        outcome = Some(answer)
        done = true
      }

      round += 1
    }

    outcome.get
  }
}
