package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.question.MetaQuestion

class MetaScheduler(mq: MetaQuestion) {
  private def done: Boolean = ???

  def run() : mq.A = {
    while (!done) {
      // resample

      // compute estimate

      // compute estimate bounds
    }

    // return answer object
    ???
  }
}
