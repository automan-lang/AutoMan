package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.logging.{LogType, LogLevelInfo, DebugLog}
import edu.umass.cs.automan.core.question.{Question, MetaQuestion}

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
