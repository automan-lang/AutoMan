package org.automanlang.core.question

package object confidence {
  sealed trait ConfidenceInterval
  case class UnconstrainedCI() extends ConfidenceInterval
  case class SymmetricCI(error: Double) extends ConfidenceInterval
  case class AsymmetricCI(low_error: Double, high_error: Double) extends ConfidenceInterval
}
