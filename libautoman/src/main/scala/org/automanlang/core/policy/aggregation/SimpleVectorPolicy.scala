package org.automanlang.core.policy.aggregation

import org.automanlang.core.logging.{LogLevelInfo, DebugLog, LogType}
import org.automanlang.core.question.VectorQuestion
import org.automanlang.core.scheduler.Task

class SimpleVectorPolicy(question: VectorQuestion)
  extends VectorPolicy(question) {

  DebugLog("Policy: simple vector",LogLevelInfo(),LogType.STRATEGY, question.id)

  override protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal): Int = {
    // additional number needed to reach num_samples
    Math.max(question.sample_size - outstanding_tasks(tasks).size, 0)
  }
}