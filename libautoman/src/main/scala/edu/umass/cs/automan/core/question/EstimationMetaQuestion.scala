package edu.umass.cs.automan.core.question

import java.util.{Date, UUID}
import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer.{AbstractEstimate, EstimationOutcome}
import edu.umass.cs.automan.core.mock.MockResponse
import edu.umass.cs.automan.core.policy.aggregation.BootstrapEstimationMetaPolicy

case class EstimationMetaQuestion(lhs: EstimationQuestion,
                                  rhs: EstimationQuestion,
                                  op: Double => Double => Double)
  extends EstimationQuestion with MetaQuestion {
  type MA = A
  type MAA = AA
  type MAP = BootstrapEstimationMetaPolicy

  private val _metaPolicy = new BootstrapEstimationMetaPolicy(lhs, rhs)

  override def memo_hash: String = lhs.memo_hash + rhs.memo_hash
  override protected[automan] def toMockResponse(question_id: UUID, response_time: Date, a: Double, worker_id: UUID): MockResponse = ???
  override protected[automan] def getOutcome(adapter: AutomanAdapter) : O = {
    EstimationOutcome(this, this.metaSchedulerFuture())
  }

  override def computeAnswer(round: Int): AbstractEstimate = _metaPolicy.computeAnswer(round)

  override protected[automan] def cloneWithConfidence(conf: Double): EstimationQuestion = {
    val lhs2 = lhs.cloneWithConfidence(Math.sqrt(conf))
    val rhs2 = rhs.cloneWithConfidence(Math.sqrt(conf))
    EstimationMetaQuestion(lhs2, rhs2, op)
  }

  override def done: Boolean = _metaPolicy.done
}
