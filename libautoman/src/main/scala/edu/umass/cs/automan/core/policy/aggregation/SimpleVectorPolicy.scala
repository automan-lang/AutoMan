package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.logging.{LogLevelInfo, DebugLog, LogType}
import edu.umass.cs.automan.core.question.VectorQuestion
import edu.umass.cs.automan.core.scheduler.Task

class SimpleVectorPolicy(question: VectorQuestion)
  extends VectorPolicy(question) {

  DebugLog("Policy: simple vector",LogLevelInfo(),LogType.STRATEGY, question.id)

  override protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal): Int = {
    // additional number needed to reach num_samples
    Math.max(question.sample_size - outstanding_tasks(tasks).size, 0)
  }
}