package org.automanlang.core.question

import java.util.{Date, UUID}
import org.automanlang.core.AutomanAdapter
import org.automanlang.core.answer.{AbstractEstimate, EstimationOutcome}
import org.automanlang.core.mock.MockResponse
import org.automanlang.core.policy.aggregation.BootstrapEstimationMetaPolicy

case class EstimationMetaQuestion(val lhs: EstimationQuestion,
                                  val rhs: EstimationQuestion,
                                  op: Double => Double => Double)
  extends EstimationQuestion with MetaQuestion {
  type MA = A
  type MAA = AA
  type MAP = BootstrapEstimationMetaPolicy

  private val _metaPolicy = new BootstrapEstimationMetaPolicy(this, op)

  override def memo_hash: String = lhs.memo_hash + rhs.memo_hash
  override protected[automanlang] def toMockResponse(question_id: UUID, response_time: Date, a: Double, worker_id: UUID): MockResponse = ???
  override protected[automanlang] def getOutcome(adapter: AutomanAdapter) : O = {
    EstimationOutcome(this, this.metaSchedulerFuture(adapter))
  }

  override def metaAnswer(round: Int, backend: AutomanAdapter): AbstractEstimate = _metaPolicy.metaAnswer(round, backend)

  override protected[automanlang] def cloneWithConfidence(conf: Double): EstimationQuestion = {
    val lhs2 = lhs.cloneWithConfidence(Math.sqrt(conf))
    val rhs2 = rhs.cloneWithConfidence(Math.sqrt(conf))
    EstimationMetaQuestion(lhs2, rhs2, op)
  }

  override def done(round: Int, backend: AutomanAdapter): Boolean = _metaPolicy.done(round, backend)
}
