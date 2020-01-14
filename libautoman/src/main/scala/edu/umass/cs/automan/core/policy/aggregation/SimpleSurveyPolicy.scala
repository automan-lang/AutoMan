package edu.umass.cs.automan.core.policy.aggregation

import edu.umass.cs.automan.core.logging.{DebugLog, LogLevelInfo, LogType}
import edu.umass.cs.automan.core.question.{Question, Survey}
import edu.umass.cs.automan.core.scheduler.Task

class SimpleSurveyPolicy(survey: Survey)
  extends SurveyPolicy(survey) {

  DebugLog("Policy: simple survey",LogLevelInfo(),LogType.STRATEGY, survey.id)

  override protected[policy] def num_to_run(tasks: List[Task], num_comparisons: Int, reward: BigDecimal): Int = {
    // additional number needed to reach num_samples
    Math.max(survey.sample_size - outstanding_tasks(tasks).size, 0)
  }

  /**
    * Returns an appropriate response for when the computation ran out of money.
    *
    * @param tasks           The complete list of tasks.
    * @param need            The smallest amount of money needed to complete the computation under optimistic assumptions.
    * @param have            The amount of money we have.
    * @param num_comparisons The number of times is_done has been called.
    * @return A low-confidence or over-budget answer.
    */
  override def select_over_budget_answer(tasks: List[Task], need: BigDecimal, have: BigDecimal, num_comparisons: Int): Question#AA = ???
}
