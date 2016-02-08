package edu.umass.cs.automan.tools

import edu.umass.cs.automan.core.policy.aggregation.AdversarialPolicy
import edu.umass.cs.automan.core.question.DiscreteScalarQuestion

class StubAdversarialPolicy(question: DiscreteScalarQuestion)
  extends AdversarialPolicy(question) {
  override protected[automan] val NumberOfSimulations: Int = 10000000
}
