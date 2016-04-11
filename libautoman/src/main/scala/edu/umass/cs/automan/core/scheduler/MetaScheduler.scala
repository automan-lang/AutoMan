package edu.umass.cs.automan.core.scheduler

import edu.umass.cs.automan.core.question.MetaQuestion

class MetaScheduler(val metaQ: MetaQuestion) {
  private def done(ans: Option[metaQ.MAA]): Boolean =
    ans match {
      case None => false
      case Some(a) => metaQ.done
    }

  def run() : metaQ.MAA = {
    var round = 1
    var answer: Option[metaQ.MAA] = None

    while (!done(answer)) {
      // compute answer
      answer = Some(metaQ.computeAnswer(round))

      // bump round
      round += 1
    }

    // return answer object
    answer match {
      case None => throw new Exception("Should not occur.")
      case Some(a) => a
    }
  }
}
