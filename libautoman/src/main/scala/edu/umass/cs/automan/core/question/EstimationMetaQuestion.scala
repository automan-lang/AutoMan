package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.answer.AbstractMultiEstimate

case class EstimationMetaQuestion(lhs: EstimationQuestion,
                                  rhs: EstimationQuestion,
                                  op: Double => Double => Double)
  extends MetaQuestion {
  type A = AbstractMultiEstimate
}
