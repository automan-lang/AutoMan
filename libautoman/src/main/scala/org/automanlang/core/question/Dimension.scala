package org.automanlang.core.question

import org.automanlang.core.question.confidence.ConfidenceInterval

object Dim {
  def mean(X: Seq[Double]) = X.sum / X.length
  def apply(id: Symbol,
            confidence_interval: ConfidenceInterval,
            min: Option[Double] = None,
            max: Option[Double] = None,
            estimator: Seq[Double] => Double = mean
           ) : Dimension = {
    Dimension(id, confidence_interval, min, max, estimator)
  }
}

case class Dimension(id: Symbol,
                     confidence_interval: ConfidenceInterval,
                     min: Option[Double],
                     max: Option[Double],
                     estimator: Seq[Double] => Double)