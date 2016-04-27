package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.question.MetaQuestion

class MetaScheduler(val metaQ: MetaQuestion, backend: AutomanAdapter) {
  def run() : metaQ.MAA = {
    var round = 1
    var done = false
    var outcome: Option[metaQ.MAA] = None

    while(!done) {
      // compute answer
      val answer = metaQ.metaAnswer(1, backend)

      // joint constraints met?
      done = !metaQ.done(round, backend)

      if (done) {
        outcome = Some(answer)
        done = true
      }

      round += 1
    }

    outcome.get
  }
}
